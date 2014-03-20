package com.opersys.processexplorer;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.*;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import com.opersys.processexplorer.node.*;

public class ProcessExplorerMain extends Activity implements NodeServiceListener {

    private static final String TAG = "ProcessExplorer";

    private NodeService nodeService;
    private NodeServiceConnection nodeServiceConn;
    private SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        findViewById(R.id.toggleNodeService).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleNodeService();
            }
        });
    }

    protected void startService() {
        Intent serviceIntent;

        // Start the master service.
        serviceIntent = new Intent(this, NodeService.class);
        nodeServiceConn = new NodeServiceConnection(this);
        bindService(serviceIntent, nodeServiceConn, Context.BIND_AUTO_CREATE);
    }

    protected void stopService() {
        unbindService(nodeServiceConn);

        nodeServiceConn = null;
        nodeService = null;
    }

    protected void toggleNodeService() {
        if (this.nodeService != null)
            stopService();
        else
            startService();
    }

    @Override
    public void onConnected(NodeService nodeService) {
        this.nodeService = nodeService;
        nodeService.start();
    }

    @Override
    public void onDisconnected() {
        this.nodeService = null;
    }

    @Override
    public void NodeServiceEvent(NodeServiceEvent ev, NodeServiceEventData evData) {
        if (ev == NodeServiceEvent.NODE_QUIT) {
            Log.w(TAG, "Node process failed, output follows");
            Log.w(TAG, "== STDOUT ==");
            Log.w(TAG, evData.getStdout());
            Log.w(TAG, "== STDERR ==");
            Log.w(TAG, evData.getStderr());
        }
    }
}
