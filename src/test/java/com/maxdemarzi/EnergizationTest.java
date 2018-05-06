package com.maxdemarzi;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.harness.junit.Neo4jRule;
import org.neo4j.test.server.HTTP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class EnergizationTest {

    @Rule
    public Neo4jRule neo4j = new Neo4jRule()
            .withFixture(MODEL_STATEMENT)
            .withExtension("/v1", Energization.class);

    @Test
    public void shouldRespondToGetEnergizedMethod() {
        HTTP.POST(neo4j.httpURI().resolve("/v1/schema/create").toString());
        HTTP.POST(neo4j.httpURI().resolve("/v1/energization/clear").toString());
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/v1/energization").toString(), input);
        ArrayList<String> actual  = response.content();
        Collections.sort(actual);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void shouldRespondToGetEnergizedSPIMethod() {
        HTTP.POST(neo4j.httpURI().resolve("/v1/schema/create").toString());
        HTTP.POST(neo4j.httpURI().resolve("/v1/energization/clear").toString());
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/v1/energization/spi").toString(), input);
        ArrayList<String> actual  = response.content();
        Collections.sort(actual);
        Assert.assertEquals(expected, actual);
    }
    @Test
    public void shouldRespondToGetEnergizedStreamingMethod() {
        HTTP.POST(neo4j.httpURI().resolve("/v1/schema/create").toString());
        HTTP.POST(neo4j.httpURI().resolve("/v1/energization/clear").toString());
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/v1/energization/streaming").toString(), input);
        ArrayList<String> actual  = response.content();
        Collections.sort(actual);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void shouldRespondToGetEnergizedStreamingMultiThreadedMethod() {
        HTTP.POST(neo4j.httpURI().resolve("/v1/schema/create").toString());
        HTTP.POST(neo4j.httpURI().resolve("/v1/energization/clear").toString());
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/v1/energization/multi").toString(), input);
        ArrayList<String> actual  = response.content();
        Collections.sort(actual);
        Assert.assertEquals(expected, actual);
    }
    

    private static final String MODEL_STATEMENT =
          /*
                                                           -on- l5 -on- e7
                                                         /
               -on- l1 -on- -> e2 {138}-on- l3 -on- -> e3 {22} -on- l6 -off- e4
             /                                          \
            e1 {138}                                      -on- l7 -on- e8 {138}
             \
                -on- l2 -on- -> e5 {22}-on- l4 -on- -> e6 {138}
         */

            "CREATE (e1:Equipment {equipment_id: 'e1', voltage:138.0})" +
                    "CREATE (e2:Equipment {equipment_id: 'e2', voltage:138.0})" +
                    "CREATE (e3:Equipment {equipment_id: 'e3', voltage:22.0})" +
                    "CREATE (e4:Equipment {equipment_id: 'e4', voltage:22.0})" +
                    "CREATE (e5:Equipment {equipment_id: 'e5', voltage:22.0})" +
                    "CREATE (e6:Equipment {equipment_id: 'e6', voltage:138.0})" +
                    "CREATE (e7:Equipment {equipment_id: 'e7', voltage:22.0})" +
                    "CREATE (e8:Equipment {equipment_id: 'e8', voltage:138.0})" +
                    "CREATE (e1)-[l1:CONNECTED {incoming_switch_on: true, outgoing_switch_on: true }]->(e2)" +
                    "CREATE (e1)-[l2:CONNECTED {incoming_switch_on: true, outgoing_switch_on: true }]->(e5)" +
                    "CREATE (e2)-[l3:CONNECTED {incoming_switch_on: true, outgoing_switch_on: true }]->(e3)" +
                    "CREATE (e5)-[l4:CONNECTED {incoming_switch_on: true, outgoing_switch_on: true }]->(e6)" +
                    "CREATE (e3)-[l5:CONNECTED {incoming_switch_on: true, outgoing_switch_on: true }]->(e7)" +
                    "CREATE (e3)-[l6:CONNECTED {incoming_switch_on: true, outgoing_switch_on: false }]->(e4)" +
                    "CREATE (e3)-[l7:CONNECTED {incoming_switch_on: true, outgoing_switch_on: true }]->(e8)"
            ;

    private static final HashMap input = new HashMap<String, Object>() {{
        put("ids", new ArrayList<String> () {{
            add("e1");
        }});
    }};


    private static final ArrayList<String> expected = new ArrayList<String>() {{
        add("e1");
        add("e2");
        add("e3");
        add("e5");
        add("e7");
    }};
}