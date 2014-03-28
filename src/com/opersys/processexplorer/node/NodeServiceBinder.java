package com.opersys.processexplorer.node;

import android.os.Binder;

public class NodeServiceBinder extends Binder {

    private NodeService target;

    public void startNodeProcess() {
        target.startNodeProcess();
    }

    public void stopNodeProcess() {

    }

    public boolean isNodeProcessRunning() {
        return target.isNodeProcessRunning();
    }

    public void addNodeServiceListener(NodeServiceListener nodeServiceListener) {
        target.addNodeServiceListener(nodeServiceListener);
    }

    public void removeNodeServiceListener(NodeServiceListener nodeServiceListener) {
        target.addNodeServiceListener(nodeServiceListener);
    }

    public NodeServiceBinder(NodeService target) {
        this.target = target;
    }
}
