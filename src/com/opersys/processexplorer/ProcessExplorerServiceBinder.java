package com.opersys.processexplorer;

import android.os.Binder;
import com.opersys.processexplorer.node.NodeThreadListener;

public class ProcessExplorerServiceBinder extends Binder {

    private ProcessExplorerService target;

    public void startServiceThreads() {
        target.startServices();
    }

    public void stopServiceThreads() {
        target.stopServices();
    }

    public boolean isNodeProcessRunning() {
        return target.isNodeProcessRunning();
    }

    public void addNodeThreadListener(NodeThreadListener serviceListener) {
        target.addNodeThreadListener(serviceListener);
    }

    public void removeNodeThreadListener(NodeThreadListener serviceListener) {
        target.addNodeThreadListener(serviceListener);
    }

    public ProcessExplorerServiceBinder(ProcessExplorerService target) {
        this.target = target;
    }
}
