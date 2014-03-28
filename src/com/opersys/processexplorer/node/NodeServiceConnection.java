package com.opersys.processexplorer.node;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class NodeServiceConnection implements ServiceConnection {

    private static final String TAG = "ProcessExplorer-NodeServiceConnection";

    private final NodeServiceListener serviceListener;

    public NodeServiceConnection(NodeServiceListener serviceListener) {
        this.serviceListener = serviceListener;
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        serviceListener.onNodeServiceConnected((NodeServiceBinder) iBinder);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        serviceListener.onNodeServiceDisconnected();
    }
}