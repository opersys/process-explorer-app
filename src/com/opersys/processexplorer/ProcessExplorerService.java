package com.opersys.processexplorer;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import com.opersys.processexplorer.node.*;
import com.opersys.processexplorer.platforminfo.PlatformInfoServer;

import java.util.ArrayList;
import java.util.List;
import java.lang.Thread;

public class ProcessExplorerService extends Service implements Thread.UncaughtExceptionHandler {

    class NodeProcessHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }

    private static final String TAG = "ProcessExplorerService";

    private List<NodeThreadListener> serviceListeners = new ArrayList<NodeThreadListener>();

    protected ProcessExplorerNotificationManager notifMgr;

    private NodeProcessThread nodeThread;
    private PlatformInfoServer platformInfoServer;

    public void fireNodeServiceEvent(NodeThreadEvent ev, NodeThreadEventData evData) {
        for (NodeThreadListener serviceListener : this.serviceListeners)
            serviceListener.ProcessExplorerServiceEvent(ev, evData);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notifMgr = new ProcessExplorerNotificationManager(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(
                notifMgr.getForegroundNotificationId(),
                notifMgr.getForegroundNotification());

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

        stopServiceThreads();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new ProcessExplorerServiceBinder(this);
    }

    public void addNodeThreadListener(NodeThreadListener serviceListener) {
        if (this.serviceListeners.contains(serviceListener))
            return;

        this.serviceListeners.add(serviceListener);
    }

    public void removeNodeThreadListener(NodeThreadListener serviceListener) {
        this.serviceListeners.remove(serviceListener);
    }

    public void startServiceThreads() {
        startNodeProcess();
        startPlatformInfoServer();
    }

    public void stopServiceThreads() {
        stopNodeProcess();
        stopPlatformInfoServer();
    }

    protected void startNodeProcess() {
        if (nodeThread != null) {
            Log.w(TAG, "Node process already started");
            return;
        }

        Log.i(TAG, "Asked to start Node process");

        nodeThread = new NodeProcessThread(getFilesDir().toString(), "node", "app.js",
                new NodeProcessHandler(),
                this);
        nodeThread.setUncaughtExceptionHandler(this);
        nodeThread.startProcess();
    }

    protected void stopNodeProcess() {
        if (nodeThread == null) return;

        Log.i(TAG, "Asked to stop Node process");

        nodeThread.stopProcess();
        nodeThread = null;
    }

    protected void startPlatformInfoServer() {
        if (platformInfoServer != null) {
            Log.w(TAG, "Platform information restlet already started");
            return;
        }

        Log.i(TAG, "Asked to start platform information restlet");

        platformInfoServer = new PlatformInfoServer();
        platformInfoServer.startServer();
    }

    protected void stopPlatformInfoServer() {
        if (platformInfoServer == null) return;

        Log.i(TAG, "Asked to stop platform information restlet");

        platformInfoServer.stopServer();
        platformInfoServer = null;
    }

    public boolean isNodeProcessRunning() {
        return nodeThread != null;
    }

    public ProcessExplorerService() {
        super();
    }
}
