package com.opersys.processexplorer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.opersys.processexplorer.node.NodeService;

public class ProcessExplorerReceiver extends BroadcastReceiver {

    private static final String TAG = "ProcessExplorerReceiver";

    private ProcessExplorerMain mainActivity;

    public ProcessExplorerReceiver(ProcessExplorerMain main) {
        this.mainActivity = main;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(NodeService.EVENT_STARTED))
            Log.i(TAG, "Node service started");

        else if (intent.getAction().equals(NodeService.EVENT_STARTING))
            Log.i(TAG, "Node service starting");

        else if (intent.getAction().equals(NodeService.EVENT_STOPPED))
            Log.i(TAG, "Node service stopped");

        mainActivity.onBroadcastReceived(intent);
    }
}
