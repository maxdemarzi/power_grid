package com.maxdemarzi;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.cursor.Cursor;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.InitialBranchState;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.neo4j.kernel.api.ReadOperations;
import org.neo4j.kernel.api.exceptions.EntityNotFoundException;
import org.neo4j.kernel.api.exceptions.index.IndexNotFoundKernelException;
import org.neo4j.kernel.api.exceptions.schema.IndexBrokenKernelException;
import org.neo4j.kernel.api.exceptions.schema.SchemaRuleNotFoundException;
import org.neo4j.kernel.api.index.IndexDescriptor;
import org.neo4j.kernel.impl.api.store.RelationshipIterator;
import org.neo4j.kernel.impl.core.ThreadToStatementContextBridge;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.storageengine.api.NodeItem;
import org.neo4j.storageengine.api.RelationshipItem;
import org.roaringbitmap.RoaringBitmap;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

@Path("/energization")
public class Energization {
    public static final int CPUS = Runtime.getRuntime().availableProcessors();

    public static final ObjectMapper objectMapper = new ObjectMapper();
    public static final RoaringBitmap energized = new RoaringBitmap();
    public static final ThreadSafeRoaringBitmap energized2 = new ThreadSafeRoaringBitmap();
    public static GraphDatabaseAPI dbapi;
    public static int equipmentLabelId;
    public static int propertyEquipmentId;
    public static int propertyIncomingSwitchOn;
    public static int propertyOutgoingSwitchOn;
    public static int propertyVoltage;

    private static final EnergizationExpander expander = new EnergizationExpander();
    private static final EnergizationEvaluator evaluator = new EnergizationEvaluator();


    public Energization(@Context GraphDatabaseService db) {
        this.dbapi = (GraphDatabaseAPI) db;
        try (Transaction tx = db.beginTx()) {
            ThreadToStatementContextBridge ctx = dbapi.getDependencyResolver().resolveDependency(ThreadToStatementContextBridge.class);
            ReadOperations ops = ctx.get().readOperations();
            equipmentLabelId = ops.labelGetForName(Labels.Equipment.name());
            propertyEquipmentId = ops.propertyKeyGetForName("equipment_id");
            propertyIncomingSwitchOn = ops.propertyKeyGetForName("incoming_switch_on");
            propertyOutgoingSwitchOn = ops.propertyKeyGetForName("outgoing_switch_on");
            propertyVoltage = ops.propertyKeyGetForName("voltage");
            tx.success();
        }
    }

    @POST
    @Path("clear")
    public Response clear(@Context GraphDatabaseService db) throws IOException {
        energized.clear();
        energized2.clear();
        Map<String, String> results = new HashMap<String, String>() {{
            put("energization", "cleared");
        }};
        return Response.ok().entity(objectMapper.writeValueAsString(results)).build();
    }

    @POST
    public Response energization(String body, @Context GraphDatabaseService db) throws IOException {
        HashMap input = Validators.getValidEquipmentIds(body);

        Set<Node> startingEquipment = new HashSet<>();
        Set results = new HashSet<>();
        ArrayList<Long> skip = new ArrayList<>();

        try (Transaction tx = db.beginTx()) {
            ((Collection) input.get("ids")).forEach((id) -> startingEquipment.add(db.findNode(Labels.Equipment, "equipment_id", id)));

            if (startingEquipment.isEmpty()) {
                throw Exceptions.equipmentNotFound;
            }

            startingEquipment.forEach(bus -> {
                InitialBranchState.State<Double> ibs;
                ibs = new InitialBranchState.State<>((Double) bus.getProperty("voltage", 999.0), 0.0);
                TraversalDescription td = db.traversalDescription()
                        .depthFirst()
                        .expand(expander, ibs)
                        .uniqueness(Uniqueness.NODE_GLOBAL)
                        .evaluator(evaluator);

                for (org.neo4j.graphdb.Path position : td.traverse(bus)) {
                    Node endNode = position.endNode();
                    if (!skip.contains(endNode.getId())) {
                        results.add(position.endNode().getProperty("equipment_id"));
                        skip.add(endNode.getId());
                    }

                    endNode.setProperty("Energized", true);
                }
            });
            tx.success();
        }
        return  Response.ok().entity(objectMapper.writeValueAsString(results)).build();
    }

    @POST
    @Path("/spi")
    public Response energizationSPI(String body, @Context GraphDatabaseService db) throws IndexNotFoundKernelException, IOException, SchemaRuleNotFoundException, IndexBrokenKernelException, EntityNotFoundException {
        HashMap input = Validators.getValidEquipmentIds(body);
        HashSet<String> results = new HashSet<>();

        try (Transaction tx = db.beginTx()) {
            ThreadToStatementContextBridge ctx = dbapi.getDependencyResolver().resolveDependency(ThreadToStatementContextBridge.class);
            ReadOperations ops = ctx.get().readOperations();
            IndexDescriptor descriptor = ops.indexGetForLabelAndPropertyKey(equipmentLabelId, propertyEquipmentId);

            HashMap<Long, Double> equipmentMap = new HashMap<>();
            for (String equipmentId : (Collection<String>) input.get("ids")) {
                Cursor<NodeItem> nodes = ops.nodeCursorGetFromUniqueIndexSeek(descriptor, equipmentId);
                if (nodes.next()) {
                    long equipmentNodeId = nodes.get().id();
                    energized.add((int) equipmentNodeId);
                    results.add(equipmentId);
                    equipmentMap.put(equipmentNodeId, (double) ops.nodeGetProperty(equipmentNodeId, propertyVoltage));

                    while (!equipmentMap.isEmpty()) {
                        Map.Entry<Long, Double> entry = equipmentMap.entrySet().iterator().next();
                        equipmentMap.remove(entry.getKey());
                        RelationshipIterator relationshipIterator = ops.nodeGetRelationships(entry.getKey(), org.neo4j.graphdb.Direction.BOTH);
                        Cursor<RelationshipItem> c;

                        while (relationshipIterator.hasNext()) {
                            c = ops.relationshipCursor(relationshipIterator.next());
                            if (c.next() && (boolean) c.get().getProperty(propertyIncomingSwitchOn) && (boolean) c.get().getProperty(propertyOutgoingSwitchOn)) {
                                long otherNodeId = c.get().otherNode(entry.getKey());
                                if (!energized.contains((int) otherNodeId)) {
                                    double newVoltage = (double) ops.nodeGetProperty(otherNodeId, propertyVoltage);
                                    if (newVoltage <= entry.getValue()) {
                                        results.add( (String)ops.nodeGetProperty(otherNodeId, propertyEquipmentId));
                                        energized.add((int) otherNodeId);
                                        equipmentMap.put(otherNodeId, newVoltage);
                                    }
                                }
                            }
                        }
                    }
                } else {
                    System.out.println("none found");
                }
                nodes.close();
            }
            tx.success();
        }

        return Response.ok().entity(objectMapper.writeValueAsString(results)).build();
    }

    @POST
    @Path("/streaming")
    public Response energizationStreaming(String body, @Context GraphDatabaseService db) throws IndexNotFoundKernelException, IOException, SchemaRuleNotFoundException, IndexBrokenKernelException, EntityNotFoundException {
        HashMap input = Validators.getValidEquipmentIds(body);
        StreamingOutput stream = os -> {
            JsonGenerator jg = objectMapper.getJsonFactory().createJsonGenerator(os, JsonEncoding.UTF8);
            jg.writeStartArray();

            try (Transaction tx = db.beginTx()) {
                ThreadToStatementContextBridge ctx = dbapi.getDependencyResolver().resolveDependency(ThreadToStatementContextBridge.class);
                ReadOperations ops = ctx.get().readOperations();
                IndexDescriptor descriptor = ops.indexGetForLabelAndPropertyKey(equipmentLabelId, propertyEquipmentId);

                HashMap<Long, Double> equipmentMap = new HashMap<>();
                for (String equipmentId : (Collection<String>) input.get("ids")) {
                    Cursor<NodeItem> nodes = ops.nodeCursorGetFromUniqueIndexSeek(descriptor, equipmentId);
                    if (nodes.next()) {
                        long equipmentNodeId = nodes.get().id();
                        energized.add((int) equipmentNodeId);
                        jg.writeString(equipmentId);
                        equipmentMap.put(equipmentNodeId, (double) ops.nodeGetProperty(equipmentNodeId, propertyVoltage));

                        while (!equipmentMap.isEmpty()) {
                            Map.Entry<Long, Double> entry = equipmentMap.entrySet().iterator().next();
                            equipmentMap.remove(entry.getKey());
                            RelationshipIterator relationshipIterator = ops.nodeGetRelationships(entry.getKey(), org.neo4j.graphdb.Direction.BOTH);
                            Cursor<RelationshipItem> c;

                            while (relationshipIterator.hasNext()) {
                                c = ops.relationshipCursor(relationshipIterator.next());
                                if (c.next() && (boolean) c.get().getProperty(propertyIncomingSwitchOn) && (boolean) c.get().getProperty(propertyOutgoingSwitchOn)) {
                                    long otherNodeId = c.get().otherNode(entry.getKey());
                                    if (!energized.contains((int) otherNodeId)) {
                                        double newVoltage = (double) ops.nodeGetProperty(otherNodeId, propertyVoltage);
                                        if (newVoltage <= entry.getValue()) {
                                            jg.writeString((String) ops.nodeGetProperty(otherNodeId, propertyEquipmentId));
                                            energized.add((int) otherNodeId);
                                            equipmentMap.put(otherNodeId, newVoltage);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    nodes.close();
                }
                tx.success();
            } catch (Exception e) {
                e.printStackTrace();
            }

            jg.writeEndArray();
            jg.flush();
            jg.close();
        };
        return Response.ok().entity(stream).type(MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("multi")
    public StreamingOutput energizationThreaded(String body, @Context GraphDatabaseService db) throws IndexNotFoundKernelException, IOException, SchemaRuleNotFoundException, IndexBrokenKernelException, EntityNotFoundException {
        System.out.println("Started: "  );
        System.out.println( new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(Calendar.getInstance().getTime()) );
        HashMap input = Validators.getValidEquipmentIds(body);
        BlockingQueue<Object[]> queue = new LinkedBlockingQueue<>();
        BlockingQueue<String> results = new LinkedBlockingQueue <>();
        ExecutorService service = Executors.newFixedThreadPool(CPUS);

        return new StreamingOutput() {
            public void write(OutputStream os) throws IOException {
                JsonGenerator jg = objectMapper.getJsonFactory().createJsonGenerator(os, JsonEncoding.UTF8);
                jg.writeStartArray();

                for (int i = 0; i < CPUS; ++i) {
                    service.execute(new Worker(queue, results));
                }

                try (Transaction tx = db.beginTx()) {
                    ThreadToStatementContextBridge ctx = dbapi.getDependencyResolver().resolveDependency(ThreadToStatementContextBridge.class);
                    ReadOperations ops = ctx.get().readOperations();
                    IndexDescriptor descriptor = ops.indexGetForLabelAndPropertyKey(equipmentLabelId, propertyEquipmentId);

                    for (String equipmentId : (Collection<String>) input.get("ids")) {
                        Cursor<NodeItem> nodes = ops.nodeCursorGetFromUniqueIndexSeek(descriptor, equipmentId);
                        if (nodes.next()) {
                            long equipmentNodeId = nodes.get().id();
                            energized2.add((int) equipmentNodeId);
                            jg.writeString(equipmentId);
                            queue.add(new Object[]{equipmentId, equipmentNodeId, ops.nodeGetProperty(equipmentNodeId, propertyVoltage)});
                        }
                        nodes.close();
                    }
                    tx.success();
                } catch (Exception e) {
                    e.printStackTrace();
                }


                do {
                    String result = null;
                    try {
                        result = results.poll(1, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (result == null) {
                        break;
                    }
                    jg.writeString(result);
                } while (true);
                jg.writeEndArray();
                jg.flush();
                jg.close();

                try {
                    service.shutdown();
                    service.awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    System.err.println("tasks interrupted");
                } finally {
                    if (!service.isTerminated()) {
                        System.err.println("cancel tasks");
                    }
                    service.shutdownNow();
                    System.out.println("shutdown finished");
                }
                System.out.println("Ended: "  );
                System.out.println( new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(Calendar.getInstance().getTime()) );

            }

        };
    }
}
