package com.opersys.processexplorer;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import com.opersys.processexplorer.node.NodeThreadEvent;
import com.opersys.processexplorer.node.NodeThreadEventData;
import com.opersys.processexplorer.node.NodeThreadListener;
import com.opersys.processexplorer.tasks.AssetExtractTask;
import com.opersys.processexplorer.tasks.AssetExtractTaskParams;

public class ProcessExplorerSettingsActivity extends PreferenceActivity
        implements NodeThreadListener {

    public static final String TAG = "ProcessExplorer";

    protected ProcessExplorerServiceBinder serviceBinder;
    protected ProcessExplorerServiceConnection servConn;

    protected void prepareLayout() {
        addPreferencesFromResource(R.xml.preferences);

        findPreference("startNow").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                serviceBinder.startServiceThreads();
                return true;
            }
        });

        findPreference("stopNow").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                serviceBinder.stopServiceThreads();
                return true;
            }
        });
    }

    protected void startService() throws Exception {
        Intent servIntent;

        servIntent = new Intent(this, ProcessExplorerService.class);
        servConn = new ProcessExplorerServiceConnection(this);

        /*
         * FIXME: I'm absolutely not sure we can call bindService immediately after bindService but
         * I haven't found anything in the documentation that says otherwise.
         */
        if (startService(servIntent) != null)
            bindService(servIntent, servConn, BIND_AUTO_CREATE);
        else
            throw new Exception("Failed to start service");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final ProgressDialog progDialog;
        AssetExtractTaskParams extractTaskParams;
        AssetExtractTask extractTask;

        super.onCreate(savedInstanceState);

        prepareLayout();

        extractTaskParams = new AssetExtractTaskParams();
        extractTaskParams.assetPath = "system-explorer.zip";
        extractTaskParams.assetMd5sumPath = "system-explorer.zip.md5sum";
        extractTaskParams.extractPath = getFilesDir();
        extractTaskParams.assetManager = getAssets();

        if (AssetExtractTask.isExtractRequired(extractTaskParams)) {

            progDialog = new ProgressDialog(this);
            progDialog.setMax(100);
            progDialog.setMessage("Extracting assets...");
            progDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

            extractTask = new AssetExtractTask() {
                @Override
                protected void onProgressUpdate(Integer... values) {
                    progDialog.setProgress(values[0]);
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    progDialog.hide();
                }
            };
            progDialog.show();
            extractTask.execute(extractTaskParams);
        }
        else
            Log.i(TAG, "Not extracting assets.");
    }

    @Override
    protected void onResume() {
        super.onResume();

        try {
            startService();
        } catch (Exception e) {
            Log.e(TAG, "Failed to start service", e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (serviceBinder != null) {
            serviceBinder.removeNodeThreadListener(this);
            unbindService(servConn);
        }
    }

    @Override
    public void onProcessServiceConnected(ProcessExplorerServiceBinder service) {
        Log.d(TAG, "Connected to Node service");

        serviceBinder = service;
        serviceBinder.addNodeThreadListener(this);
    }

    @Override
    public void onProcessServiceDisconnected() {
        Log.d(TAG, "Disconnected from Node service");

        serviceBinder = null;
    }

    @Override
    public void ProcessExplorerServiceEvent(NodeThreadEvent ev, NodeThreadEventData evData) {
        switch (ev) {
            case NODE_STARTED:
                Log.d(TAG, "Received NODE_STARTED");
                break;

            case NODE_QUIT:
                Log.d(TAG, "Received NODE_QUIT");
                break;

            case NODE_ERROR:
                Log.d(TAG, "Received NODE_ERROR");
                break;

            case NODE_STARTING:
                Log.d(TAG, "Received NODE_STARTING");
                break;

            case NODE_STOPPING:
                Log.d(TAG, "Received NODE_STOPPING");
                break;

            case NODE_STOPPED:
                Log.d(TAG, "Received NODE_STOPPED");
                break;
        }
    }
}
