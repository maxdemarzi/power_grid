package com.maxdemarzi;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.BranchState;

import java.util.ArrayList;

public class EnergizationExpander implements PathExpander<Double> {
    @Override
    public Iterable<Relationship> expand(Path path, BranchState<Double> branchState) {
        ArrayList<Relationship> rels = new ArrayList<>();
        Node endNode = path.endNode();
        Double voltage = (Double) endNode.getProperty("voltage", 999.0);
        if (voltage <= branchState.getState()) {
            // Set the new voltage
            branchState.setState(voltage);

            endNode.getRelationships(Direction.OUTGOING, RelationshipTypes.CONNECTED).forEach(rel -> {
                if ((Boolean)rel.getProperty("incoming_switch_on", false) &&
                    (Boolean)rel.getProperty("outgoing_switch_on", false)) {
                    rels.add(rel);
                }
            });
        }

        return rels;
    }

    @Override
    public PathExpander<Double> reverse() {
        return null;
    }
}
