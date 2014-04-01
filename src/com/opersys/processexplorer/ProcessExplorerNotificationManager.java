package com.opersys.processexplorer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import com.opersys.processexplorer.node.NodeThreadEvent;
import com.opersys.processexplorer.node.NodeThreadEventData;
import com.opersys.processexplorer.node.NodeThreadListener;

/**
 * Date: 27/03/14
 * Time: 1:16 PM
 */
public class ProcessExplorerNotificationManager implements NodeThreadListener {

    private static final int SERVICE_NOTIFICATION_ID = 1;

    protected NotificationCompat.Builder notifBuilder;
    protected NotificationManager notifService;

    public ProcessExplorerNotificationManager(ProcessExplorerService nodeService) {
        Intent notifIntent;
        PendingIntent notifPendingIntent;

        notifService = (NotificationManager) nodeService.getSystemService(Context.NOTIFICATION_SERVICE);
        notifIntent = new Intent(nodeService, ProcessExplorerSettingsActivity.class);
        notifPendingIntent = PendingIntent.getActivity(
                nodeService, 0, notifIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notifBuilder = new NotificationCompat.Builder(nodeService)
                .setContentText("Process Explorer")
                .setContentIntent(notifPendingIntent)
                .setSmallIcon(R.drawable.icon)
                .setOngoing(true);

        nodeService.addNodeThreadListener(this);
    }

    public int getForegroundNotificationId() {
        return SERVICE_NOTIFICATION_ID;
    }

    // Returns the notification object the service can use to call startForeground.
    public Notification getForegroundNotification() {
        notifBuilder.setContentText("Stopped. Ready to start.");
        return notifBuilder.build();
    }

    @Override
    public void ProcessExplorerServiceEvent(NodeThreadEvent ev, NodeThreadEventData evData) {
        Notification notif;
        String contentText = null;

        if (ev == NodeThreadEvent.NODE_STARTING)
            contentText = "Starting";
        else if (ev == NodeThreadEvent.NODE_STARTED)
            contentText = "Started";
        else if (ev == NodeThreadEvent.NODE_ERROR || ev == NodeThreadEvent.NODE_STOPPED)
            contentText = "Stopped";
        else
            return;

        // Temporary thing.
        notifBuilder.setTicker(contentText);
        notifBuilder.setContentText(contentText);

        notif = notifBuilder.build();
        notifService.notify(SERVICE_NOTIFICATION_ID, notif);
    }

    @Override
    public void onProcessServiceConnected(ProcessExplorerServiceBinder service) {}

    @Override
    public void onProcessServiceDisconnected() {}
}
