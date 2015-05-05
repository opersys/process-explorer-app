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
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

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

    private static final String TAG = "ProcessExplorerService";
    private static final int SERVICE_NOTIF_ID = 1;

    private List<NodeThreadListener> serviceListeners = new ArrayList<NodeThreadListener>();

    protected ProcessExplorerNotificationManager notifMgr;

    protected NotificationCompat.Builder notifBuilder;

    private NodeProcessThread nodeThread;
    private PlatformInfoServer platformInfoServer;

    public void fireNodeServiceEvent(NodeThreadEvent ev, NodeThreadEventData evData) {
        for (NodeThreadListener serviceListener : this.serviceListeners)
            serviceListener.ProcessExplorerServiceEvent(ev, evData);
    }

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

    @Override
    public void onCreate() {
        super.onCreate();
        notifMgr = new ProcessExplorerNotificationManager(this);
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
                sharedPrefs.getBoolean("asRoot", false),
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
        SharedPreferences sharedPrefs;
        String msg;

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        switch (ev) {
            case NODE_STARTED:
                startForeground(
                        notifMgr.getForegroundNotificationId(),
                        notifMgr.getForegroundNotification());
                break;

            case NODE_STOPPED:
                break;

            case NODE_ERROR:
                nodeThread = null;
                if (sharedPrefs.getBoolean("asRoot", false)) {
                    msg = "Could not start the Process Explorer service as root. Check that root access is allowed.";
                }
                else {
                    msg = "Could not start the Process Explorer service. Check the logcat for more informations.";
                }

                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        }
    }

    public ProcessExplorerService() {
        super();
    }
}
