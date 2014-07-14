/*
* Copyright (C) 2014 Opersys inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.opersys.processexplorer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.opersys.processexplorer.node.NodeProcessThread;
import com.opersys.processexplorer.node.NodeThreadEvent;
import com.opersys.processexplorer.node.NodeThreadEventData;
import com.opersys.processexplorer.node.NodeThreadListener;
import com.opersys.processexplorer.platforminfo.PlatformInfoServer;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: François-Denis Gonthier (francois-denis.gonthier@opersys.com)
 */
public class ProcessExplorerService extends Service implements Thread.UncaughtExceptionHandler, NodeThreadListener {

    class NodeProcessHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }

    private static final int SERVICE_NOTIF_ID = 1;
    private static final String SERVICE_NOTIF_STATE_TAG = "state";

    private static final String TAG = "ProcessExplorerService";

    protected List<NodeThreadListener> serviceListeners = new ArrayList<NodeThreadListener>();
    protected NodeProcessThread nodeThread;
    protected PlatformInfoServer platformInfoServer;
    protected NotificationCompat.Builder notifBuilder;
    protected NotificationManager notifManager;
    protected Bitmap notifIcon;

    protected String getStatusToString(NodeThreadEvent ev) {
        String contentText = null;

        if (ev == NodeThreadEvent.NODE_STARTING)
            contentText = "Starting";
        else if (ev == NodeThreadEvent.NODE_STARTED)
            contentText = "Started";
        else if (ev == NodeThreadEvent.NODE_STOPPED || ev == NodeThreadEvent.NODE_ERROR)
            contentText = "Stopped";

        return contentText;
    }

    public void fireNodeServiceEvent(NodeThreadEvent ev, NodeThreadEventData evData) {
        for (NodeThreadListener serviceListener : this.serviceListeners)
            serviceListener.ProcessExplorerServiceEvent(ev, evData);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //notifMgr = new ProcessExplorerNotificationManager(this);

        String contentText = null;
        Intent notifIntent;
        PendingIntent notifPendingIntent;

        notifIntent = new Intent(this, ProcessExplorerSettingsActivity.class);
        notifPendingIntent = PendingIntent.getActivity(this, 0, notifIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        this.notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        this.notifIcon = BitmapFactory.decodeResource(getResources(), R.drawable.icon_48x48_launcher);

        notifBuilder = new NotificationCompat.Builder(this)
                .setContentText("Process Explorer")
                .setContentIntent(notifPendingIntent)
                .setSmallIcon(R.drawable.icon_24x24_notif)
                .setLargeIcon(notifIcon)
                .setOngoing(true)
                .setContentTitle("Process Explorer");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean isBooting = false;

        // If this was called by the boot broadcast receiver, start the
        // node service immediately.
        if (intent != null && intent.getExtras() != null) {

            if (intent.getExtras().containsKey("booting")) {
                Object objBooting;

                objBooting = intent.getExtras().get("booting");
                isBooting = (Boolean)objBooting;
            }
        }

        if (isBooting)
            startServices();

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

        stopServices();
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

    public void startServices() {
        startNodeProcess();
        startPlatformInfoServer();
    }

    public void stopServices() {
        stopNodeProcess();
        stopPlatformInfoServer();
    }

    protected void startNodeProcess() {
        SharedPreferences sharedPrefs;

        if (nodeThread != null) {
            Log.w(TAG, "Node process already started");
            return;
        }

        Log.i(TAG, "Asked to start Node process");

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        nodeThread = new NodeProcessThread(getFilesDir().toString(), "node", "app.js",
                new NodeProcessHandler(),
                this);

        addNodeThreadListener(this);

        nodeThread.setEnvironment("PORT", sharedPrefs.getString("nodePort", "3000"));
        nodeThread.setEnvironment("ENV", "production");
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

        platformInfoServer = new PlatformInfoServer(getPackageManager());
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

    @Override
    public void onProcessServiceConnected(ProcessExplorerServiceBinder service) {}

    @Override
    public void onProcessServiceDisconnected() {}

    @Override
    public void ProcessExplorerServiceEvent(NodeThreadEvent ev, NodeThreadEventData evData) {
        Notification notif;

        switch (ev) {
            case NODE_STARTED:
                notifBuilder
                        .setTicker(getStatusToString(ev))
                        .setContentText(getStatusToString(ev));
                notif = notifBuilder.build();

                //notifManager.notify(SERVICE_NOTIF_STATE_TAG, SERVICE_NOTIF_ID, notif);
                startForeground(SERVICE_NOTIF_ID, notif);
                break;

            case NODE_ERROR:
            case NODE_STOPPED:
                nodeThread = null;
                stopForeground(true);
                break;
        }
    }

    public ProcessExplorerService() {
        super();
    }
}
