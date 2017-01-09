package com.maxdemarzi;

import org.neo4j.cursor.Cursor;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.api.ReadOperations;
import org.neo4j.kernel.api.exceptions.EntityNotFoundException;
import org.neo4j.kernel.impl.api.store.RelationshipIterator;
import org.neo4j.kernel.impl.core.ThreadToStatementContextBridge;
import org.neo4j.storageengine.api.RelationshipItem;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

import static com.maxdemarzi.Energization.energized2;

public class Worker  implements Runnable {

    private BlockingQueue<Work> processQueue = null;
    private BlockingQueue<String> results = null;
    private int count = 0;
    private RelationshipIterator relationshipIterator;
    Cursor<RelationshipItem> c;

    public Worker(BlockingQueue<Work> processQueue, BlockingQueue<String> results) throws IOException {
        this.processQueue = processQueue;
        this.results = results;
    }

    @Override
    public void run() {
        try (Transaction tx = Energization.dbapi.beginTx()) {
            ThreadToStatementContextBridge ctx = Energization.dbapi.getDependencyResolver().resolveDependency(ThreadToStatementContextBridge.class);
            ReadOperations ops = ctx.get().readOperations();

            do {
                Work item = this.processQueue.take();
                this.processEntry(item, ops);
                count++;
            } while (true);
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(Thread.currentThread() + ": processed " + count + " entries");

    }

    private void processEntry(Work work, ReadOperations ops) throws EntityNotFoundException, InterruptedException, IOException {
        relationshipIterator = ops.nodeGetRelationships(work.getNodeId(), org.neo4j.graphdb.Direction.BOTH);

        while (relationshipIterator.hasNext()) {
            c = ops.relationshipCursor(relationshipIterator.next());
            if (c.next()
                    && (boolean) c.get().getProperty( Energization.propertyIncomingSwitchOn)
                    && (boolean) c.get().getProperty( Energization.propertyOutgoingSwitchOn)) {
                long otherNodeId = c.get().otherNode(work.getNodeId());
                if (!energized2.contains((int) otherNodeId)) {
                    double newVoltage = (double) ops.nodeGetProperty(otherNodeId,  Energization.propertyVoltage);
                    if (newVoltage <= (double) work.getVoltage()) {
                        if(energized2.checkedAdd((int) otherNodeId)) {
                            results.add((String) ops.nodeGetProperty(otherNodeId, Energization.propertyEquipmentId));
                            processQueue.put(new Work(otherNodeId, newVoltage));
                        }
                    }
                }
            }
        }
    }
}