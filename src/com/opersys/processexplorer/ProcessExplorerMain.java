package com.opersys.processexplorer;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import com.opersys.processexplorer.node.*;

public class ProcessExplorerMain extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = "ProcessExplorer";

    private ProcessExplorerReceiver bcReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final SharedPreferences sharedPrefs;
        IntentFilter bcFilter;
        Intent bcIntent;
        Preference prefStart;

        super.onCreate(savedInstanceState);

        bcFilter = new IntentFilter();
        bcReceiver = new ProcessExplorerReceiver(this);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        bcFilter.addAction(NodeService.EVENT_STARTED);
        bcFilter.addAction(NodeService.EVENT_STARTING);
        bcFilter.addAction(NodeService.EVENT_STOPPED);
        bcFilter.addAction(NodeService.EVENT_ERROR);
        bcFilter.addAction(NodeService.EVENT_STATUS);

        addPreferencesFromResource(R.xml.preferences);
        registerReceiver(bcReceiver, bcFilter);
        prefStart = (Preference) findPreference("isRunning");
        prefStart.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (sharedPrefs.getBoolean(preference.getKey(), false))
                    startService();
                else
                    stopService();

                return false;
            }
        });

        bcIntent = new Intent(this, NodeService.class);
        bcIntent.setAction(NodeService.COMMAND_STATUS);
        startService(bcIntent);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPrefs, String key) {
        Log.d(TAG, "Preference: " + key + " changing.");

        if (key.equals("isRunning")) {
            if (!sharedPrefs.contains("isRunning"))
                return;

            if (sharedPrefs.getBoolean("isRunning", false))
                startService();
            else
                stopService();
        }
    }

    protected void startService() {
        Intent serviceIntent;

        serviceIntent = new Intent(this, NodeService.class);
        serviceIntent.setAction(NodeService.COMMAND_START);
        startService(serviceIntent);
    }

    protected void stopService() {
        Intent serviceIntent;

        serviceIntent = new Intent(this, NodeService.class);
        stopService(serviceIntent);
    }

    @Override
    protected void onDestroy() {
        super.onStop();

        unregisterReceiver(bcReceiver);
    }

    public void onBroadcastReceived(Intent intent) {
        CheckBoxPreference isRunningPref = (CheckBoxPreference) findPreference("isRunning");

        if (intent.getAction().equals(NodeService.EVENT_STATUS)) {
            Log.d(TAG, "Service running == " + intent.getExtras().get("status"));
            isRunningPref.setChecked((Boolean) intent.getExtras().get("status"));
        }
    }
}
