package com.maxdemarzi;

import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.PathEvaluator;

public class EnergizationEvaluator implements PathEvaluator<Double> {
    @Override
    public Evaluation evaluate(Path path, BranchState<Double> branchState) {
        // Path with just the single node, ignore it and continue
        if (path.length() == 0 ) {
            return Evaluation.INCLUDE_AND_CONTINUE;
        }
        // Make sure last Equipment voltage is equal to or lower than previous voltage
        Double voltage = (Double) path.endNode().getProperty("voltage", 999.0);
        if (voltage <= branchState.getState()) {
            return Evaluation.INCLUDE_AND_CONTINUE;
        } else {
            return Evaluation.EXCLUDE_AND_PRUNE;
        }
    }

    @Override
    public Evaluation evaluate(Path path) {
        return null;
    }
}
