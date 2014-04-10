package com.opersys.processexplorer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Date: 09/04/14
 * Time: 10:36 PM
 */
public class ProcessExplorerBootReceiver extends BroadcastReceiver {

    private static String TAG = "ProcessExplorerBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent servIntent;
        ProcessExplorerServiceConnection servConn;
        SharedPreferences sharedPrefs;

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (sharedPrefs.getBoolean("autoStart", false)) {
            servIntent = new Intent(context, ProcessExplorerService.class);

            // Make sure the service understands that we are booting and won't
            // be binding to it but that we want the Node service to start.
            servIntent.putExtra("booting", true);

            /*
             * FIXME: I'm absolutely not sure we can call bindService immediately after bindService but
             * I haven't found anything in the documentation that says otherwise.
            */
            if (context.startService(servIntent) == null)
                Log.w(TAG, "Failed to bind to service");
        }
    }
}
