package com.maxdemarzi;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;

import java.util.Iterator;

public class EnergizationEvaluator implements Evaluator {
    @Override
    public Evaluation evaluate(Path path) {
        // Path with just the single node, ignore it and continue
        if (path.length() == 0 ) {
            return Evaluation.INCLUDE_AND_CONTINUE;
        }

        Iterator<Node> nodes = path.reverseNodes().iterator();
        Node lastEquipment = nodes.next();
        Node penultimateEquipment = nodes.next();

        // Make sure last Equipment voltage is equal to or lower than penultimate Equipment voltage
        if ((Double)lastEquipment.getProperty("voltage", 999.0) <= (Double)penultimateEquipment.getProperty("voltage", 999.0)) {
            return Evaluation.INCLUDE_AND_CONTINUE;
        } else {
            return Evaluation.EXCLUDE_AND_PRUNE;
        }

    }
}
