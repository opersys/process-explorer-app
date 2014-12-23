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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Author: François-Denis Gonthier (francois-denis.gonthier@opersys.com)
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
