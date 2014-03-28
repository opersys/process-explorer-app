package com.opersys.processexplorer.node;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.lang.Thread;

public class NodeService extends Service implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "ProcessExplorer-NodeService";

    private List<NodeServiceListener> nodeServiceListeners = new ArrayList<NodeServiceListener>();

    protected NodeServiceNotificationManager notifMgr;

    class NodeProcessHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }

    private NodeProcessThread nodeThread;

    public void fireNodeServiceEvent(NodeServiceEvent ev, NodeServiceEventData evData) {
        if (ev == NodeServiceEvent.NODE_STARTED) {
            startForeground(
                    notifMgr.getForegroundNotificationId(),
                    notifMgr.getForegroundNotification());
        }

        for (NodeServiceListener nodeServiceListener : nodeServiceListeners)
            nodeServiceListener.NodeServiceEvent(ev, evData);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notifMgr = new NodeServiceNotificationManager(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        nodeThread = null;
        Log.e(TAG, "Uncaught exception in thread, stopping the service.", ex);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.i(TAG, "Node service is being destroyed");

        if (nodeThread != null) {
            nodeThread.endProcess();
            nodeThread = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new NodeServiceBinder(this);
    }

    public void addNodeServiceListener(NodeServiceListener nodeServiceListener) {
        nodeServiceListeners.add(nodeServiceListener);
    }

    public void removeNodeServiceListener(NodeServiceListener nodeServiceListener) {
        nodeServiceListeners.remove(nodeServiceListener);
    }

    public void startNodeProcess() {
        if (nodeThread != null) {
            Log.w(TAG, "Node process already running, not restarting it.");
            return;
        }

        Log.i(TAG, "Node process starting");

        nodeThread = new NodeProcessThread(getAssets(), getFilesDir().toString(), "node", "app.js",
                new NodeProcessHandler(),
                this);
        nodeThread.setUncaughtExceptionHandler(this);
        nodeThread.start();
    }

    public void stopNodeProcess() {

    }

    public boolean isNodeProcessRunning() {
        return nodeThread != null;
    }

    public NodeService() {
        super();
    }
}
