package com.maxdemarzi;

public class Work {

    long nodeId;
    Double voltage;

    public Work(long nodeId, Double voltage) {
        this.nodeId = nodeId;
        this.voltage = voltage;
    }

    public long getNodeId() {
        return nodeId;
    }

    public Double getVoltage() {
        return voltage;
    }

}
