package com.opersys.processexplorer.node;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
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

    private static final int SERVICE_NOTIFICATION_ID = 1;

    private static final String TAG = "ProcessExplorer-NodeService";

    public static final String COMMAND_START = "com.opersys.processexplorer.node.NodeService.CMD_START";
    public static final String COMMAND_STATUS = "com.opersys.processexplorer.node.NodeService.CMD_STATUS";

    public static final String EVENT_STARTING = "com.opersys.processexplorer.node.NodeService.STARTING";
    public static final String EVENT_STARTED = "com.opersys.processexplorer.node.NodeService.STARTED";
    public static final String EVENT_STOPPED = "com.opersys.processexplorer.node.NodeService.STOPPED";
    public static final String EVENT_ERROR = "com.opersys.processexplorer.node.NodeService.ERROR";
    public static final String EVENT_STATUS = "com.opersys.processexplorer.node.NodeService.STATUS";

    private int requestCode = 0;

    private NotificationCompat.Builder notifBuilder;

    class NodeProcessHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }

    private NodeProcessThread nodeThread;

    private void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;

        while((read = in.read(buffer)) != -1)
            out.write(buffer, 0, read);
    }

    public void broadcastNodeServiceEvent(String actionName) {
        Intent eventIntent;
        NotificationManager notifMgr;

        notifMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (actionName.equals(EVENT_STARTED)) {
            notifBuilder.setTicker("Started");
            notifBuilder.setContentText("Started");
        }
        else if (actionName.equals(EVENT_STOPPED)) {
            notifBuilder.setTicker("Stopped");
            notifBuilder.setContentText("Stopped");
        }
        else if (actionName.equals(EVENT_STARTING)) {
            notifBuilder.setTicker("Starting...");
            notifBuilder.setContentText("Starting...");
        }

        notifMgr.notify(SERVICE_NOTIFICATION_ID, notifBuilder.build());

        eventIntent = new Intent();
        eventIntent.setAction(actionName);
        sendBroadcast(eventIntent);
    }

    public void start() {
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
    }

    @Override
    public void onCreate() {
        Intent notifIntent;
        PendingIntent notifPendingIntent;

        super.onCreate();

        notifIntent = new Intent(this, ProcessExplorerMain.class);
        notifPendingIntent = PendingIntent.getActivity(
                this, ++requestCode, notifIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notifBuilder = new NotificationCompat.Builder(this)
                .setContentText("Process Explorer")
                .setContentIntent(notifPendingIntent)
                .setSmallIcon(R.drawable.icon)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent bcIntent;

        if (intent.getAction().equals(COMMAND_STATUS)) {
            bcIntent = new Intent(EVENT_STATUS);
            bcIntent.putExtra("status", nodeThread != null);
            sendBroadcast(bcIntent);

            // Only stop the service if there is no thread running.
            if (nodeThread == null)
                stopSelfResult(startId);
        }
        else if (intent.getAction().equals(COMMAND_START)) {
            start();
            startForeground(1, notifBuilder.build());

            return START_STICKY;
        }
        else
            throw new Error("Invalid service command");

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
        return null;
    }

    public NodeService() {
        super();
    }
}
