package com.opersys.processexplorer;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.webkit.WebView;
import com.opersys.processexplorer.node.NodeService;
import com.opersys.processexplorer.node.NodeServiceEvent;
import com.opersys.processexplorer.node.NodeServiceEventData;
import com.opersys.processexplorer.node.NodeServiceListener;

public class ProcessExplorerMain extends Activity implements NodeServiceListener {

    class NodeServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.i(TAG, "Attached to Node service");
            nodeService = ((NodeService.NodeServiceBinder) iBinder).getService();
            nodeService.addNodeServiceListener(ProcessExplorerMain.this);
            nodeService.start();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(TAG, "Dettached from Node service");
            nodeService = null;
        }
    }

    private static final String TAG = "ProcessExplorer";

    private NodeService nodeService;

    private SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent serviceIntent, startNodeIntent;
        NodeServiceConnection nodeServiceConn;

        super.onCreate(savedInstanceState);

        //settings = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());

        // Start the master service.
        serviceIntent = new Intent(this, NodeService.class);
        nodeServiceConn = new NodeServiceConnection();
        bindService(serviceIntent, nodeServiceConn, Context.BIND_AUTO_CREATE);

        setContentView(R.layout.main);
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

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        WebView wv = (WebView) findViewById(R.id.webview);

        wv.loadUrl("http://localhost:1337/");
    }
}
