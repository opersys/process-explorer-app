package com.opersys.processexplorer.node;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import com.opersys.processexplorer.ProcessExplorerSettingsActivity;
import com.opersys.processexplorer.R;

/**
 * Date: 27/03/14
 * Time: 1:16 PM
 */
public class NodeServiceNotificationManager implements NodeServiceListener {

    private static final int SERVICE_NOTIFICATION_ID = 1;

    protected NotificationCompat.Builder notifBuilder;
    protected NotificationManager notifService;

    public NodeServiceNotificationManager(NodeService nodeService) {
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

        nodeService.addNodeServiceListener(this);
    }

    public int getForegroundNotificationId() {
        return SERVICE_NOTIFICATION_ID;
    }

    // Returns the notification object the service can use to call startForeground.
    public Notification getForegroundNotification() {
        notifBuilder.setContentText("Started");
        return notifBuilder.build();
    }

    @Override
    public void NodeServiceEvent(NodeServiceEvent ev, NodeServiceEventData evData) {
        Notification notif;

        if (ev == NodeServiceEvent.NODE_STARTING) {
            notifBuilder.setContentText("Starting...");
            notif = notifBuilder.build();

            notifService.notify(SERVICE_NOTIFICATION_ID, notif);
        }
    }

    @Override
    public void onNodeServiceConnected(NodeServiceBinder service) {}

    @Override
    public void onNodeServiceDisconnected() {}
}
