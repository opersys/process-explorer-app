package com.opersys.processexplorer.node;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import com.opersys.processexplorer.ProcessExplorerMain;
import com.opersys.processexplorer.R;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.lang.Thread;

public class NodeService extends Service implements Thread.UncaughtExceptionHandler {

    private static final int SERVICE_NOTIFICATION_ID = 255;

    class NodeProcessHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }

    private static final String TAG = "ProcessExplorer-NodeService";

    private NodeProcessThread nodeThread;

    private NodeServiceBinder nodeServiceBinder;

    private List<NodeServiceListener> nodeServiceListeners;

    private void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;

        while((read = in.read(buffer)) != -1)
            out.write(buffer, 0, read);
    }

    protected void fireNodeServiceEvent(NodeServiceEvent ev, NodeServiceEventData evData) {
        for (NodeServiceListener nsListener : nodeServiceListeners)
            nsListener.NodeServiceEvent(ev, evData);
    }

    public void addNodeServiceListener(NodeServiceListener nsListener) {
        nodeServiceListeners.add(nsListener);
    }

    public void removeNodeServiceListener(NodeServiceListener nsListener) {
        nodeServiceListeners.remove(nsListener);
    }

    public void start() {
        Notification serviceNotif;
        PendingIntent notifPendingIntent;
        Intent notifIntent;

        if (nodeThread != null) {
            Log.i(TAG, "Node service already started.");
            return;
        }

        Log.i(TAG, "Node service starting");

        nodeThread = new NodeProcessThread(getAssets(), getFilesDir().toString(), "node", "app.js",
                new NodeProcessHandler(),
                this);
        nodeThread.setUncaughtExceptionHandler(this);
        nodeThread.start();

        notifIntent = new Intent(this, ProcessExplorerMain.class);
        notifPendingIntent = PendingIntent.getActivity(this, 0, notifIntent, 0);

        serviceNotif = new Notification.Builder(this)
                .setContentTitle("Starting...")
                .setContentText("Process Explorer")
                .setContentIntent(notifPendingIntent)
                .setSmallIcon(R.drawable.icon)
                .build();

        startForeground(SERVICE_NOTIFICATION_ID, serviceNotif);
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
        return nodeServiceBinder;
    }

    public NodeService() {
        super();

        nodeServiceBinder= new NodeServiceBinder(this);
        nodeServiceListeners = new LinkedList<NodeServiceListener>();
    }
}
