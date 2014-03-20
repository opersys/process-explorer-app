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
        Log.i(TAG, "Attached to Node service");

        serviceListener.onConnected(((NodeServiceBinder) iBinder).getService());
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.i(TAG, "Dettached from Node service");

        serviceListener.onDisconnected();
    }
}