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
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import com.opersys.processexplorer.node.NodeThreadEvent;
import com.opersys.processexplorer.node.NodeThreadEventData;
import com.opersys.processexplorer.node.NodeThreadListener;

/**
 * Author: François-Denis Gonthier (francois-denis.gonthier@opersys.com)
 * Date: 27/03/14
 * Time: 1:16 PM
 */
public class ProcessExplorerNotificationManager implements NodeThreadListener {

    private static final int SERVICE_NOTIFICATION_ID = 1;

    protected NotificationCompat.Builder notifBuilder;
    protected NotificationManager notifService;
    protected Notification notif;

    public ProcessExplorerNotificationManager(ProcessExplorerService nodeService) {
        Intent notifIntent;
        PendingIntent notifPendingIntent;

        notifService = (NotificationManager) nodeService.getSystemService(Context.NOTIFICATION_SERVICE);
        notifIntent = new Intent(nodeService, ProcessExplorerSettingsActivity.class);
        notifPendingIntent = PendingIntent.getActivity(
                nodeService, 0, notifIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Bitmap bm = BitmapFactory.decodeResource(nodeService.getResources(), R.drawable.icon_48x48_launcher);

        notifBuilder = new NotificationCompat.Builder(nodeService)
                .setContentText("Process Explorer")
                .setContentIntent(notifPendingIntent)
                .setSmallIcon(R.drawable.icon_24x24_notif)
                .setLargeIcon(bm)
                .setOngoing(true);

        nodeService.addNodeThreadListener(this);
    }

    public int getForegroundNotificationId() {
        return SERVICE_NOTIFICATION_ID;
    }

    // Returns the notification object the service can use to call startForeground.
    public Notification getForegroundNotification() {
        if (notif == null)
            notif = notifBuilder.build();

        return notif;
    }

    @Override
    public void ProcessExplorerServiceEvent(NodeThreadEvent ev, NodeThreadEventData evData) {
        Notification notif;
        String contentText = null;

        if (ev == NodeThreadEvent.NODE_STARTING)
            contentText = "Starting";
        else if (ev == NodeThreadEvent.NODE_STARTED)
            contentText = "Started";
        else if (ev == NodeThreadEvent.NODE_STOPPED || ev == NodeThreadEvent.NODE_ERROR)
            contentText = "Stopped";
        else
            return;

        // Temporary thing.
        notifBuilder.setTicker(contentText);
        notifBuilder.setContentText(contentText);
        notifBuilder.setContentTitle("Process Explorer");

        notif = notifBuilder.build();
        notifService.notify(SERVICE_NOTIFICATION_ID, notif);
    }

    @Override
    public void onProcessServiceConnected(ProcessExplorerServiceBinder service) {}

    @Override
    public void onProcessServiceDisconnected() {}
}
