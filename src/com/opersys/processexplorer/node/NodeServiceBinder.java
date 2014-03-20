package com.opersys.processexplorer.node;

import android.os.Binder;

public class NodeServiceBinder extends Binder {

    private NodeService target;

    public NodeService getService() {
        return target;
    }

    public NodeServiceBinder(NodeService target) {
        this.target = target;
    }
}
