package com.opersys.processexplorer;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import com.opersys.processexplorer.node.NodeThreadListener;

public class ProcessExplorerServiceConnection implements ServiceConnection {

    private static final String TAG = "ProcessExplorer-ProcessExplorerServiceConnection";

    private final NodeThreadListener serviceListener;

    public ProcessExplorerServiceConnection(NodeThreadListener serviceListener) {
        this.serviceListener = serviceListener;
    }

    public ProcessExplorerServiceConnection() {
        this.serviceListener = null;
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        if (serviceListener != null)
            serviceListener.onProcessServiceConnected((ProcessExplorerServiceBinder) iBinder);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        if (serviceListener != null)
            serviceListener.onProcessServiceDisconnected();
    }
}