package com.opersys.processexplorer;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;
import com.opersys.processexplorer.node.*;
import com.opersys.processexplorer.platforminfo.PlatformInfoService;
import com.opersys.processexplorer.tasks.AssetExtractTask;
import com.opersys.processexplorer.tasks.AssetExtractTaskParams;

import java.net.InetAddress;

public class ProcessExplorerSettingsActivity extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener, NodeServiceListener {

    public static final String TAG = "ProcessExplorer";

    protected NodeServiceBinder nodeService;
    protected NodeServiceConnection nodeServiceConnection;

    protected void prepareLayout() {
        final SharedPreferences sharedPrefs;
        Preference prefStart;
        LayoutInflater layoutInflater;

        layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        getListView().addFooterView(layoutInflater.inflate(R.layout.main_footer, null));

        findViewById(R.id.btnStartNow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nodeService.startNodeProcess();
            }
        });

        addPreferencesFromResource(R.xml.preferences);
        prefStart = findPreference("isRunning");
/*        prefStart.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (sharedPrefs.getBoolean(preference.getKey(), false))
                    startService();
                else
                    stopService();

                return false;
            }
        });*/
    }

    protected void startServices() {
        Intent nodeServIntent, nodePlatIntent;

        nodeServIntent = new Intent(this, NodeService.class);
        nodeServiceConnection = new NodeServiceConnection(this);

        startService(nodeServIntent);
        bindService(nodeServIntent, nodeServiceConnection, BIND_AUTO_CREATE);

        nodePlatIntent = new Intent(this, PlatformInfoService.class);
        startService(nodePlatIntent);
        //bindService(nodePlatIntent, new PlatformInfoServiceConnection(this), BIND_AUTO_CREATE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final ProgressDialog progDialog;
        AssetExtractTaskParams extractTaskParams;
        AssetExtractTask extractTask;

        super.onCreate(savedInstanceState);

        prepareLayout();
        startServices();

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
        else Log.i(TAG, "Not extracting assets.");
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPrefs, String key) {
        Log.d(TAG, "Preference: " + key + " changing.");

        /*if (key.equals("isRunning")) {
            if (!sharedPrefs.contains("isRunning"))
                return;

            if (sharedPrefs.getBoolean("isRunning", false))
                startService();
            else
                stopService();
        } */
    }

    @Override
    protected void onDestroy() {
        super.onStop();

        nodeService.removeNodeServiceListener(this);
        unbindService(nodeServiceConnection);
    }

    @Override
    public void onNodeServiceConnected(NodeServiceBinder service) {
        Log.d(TAG, "Connected to Node service");

        nodeService = service;
        nodeService.addNodeServiceListener(this);
    }

    @Override
    public void onNodeServiceDisconnected() {
        Log.d(TAG, "Disconnected from Node service");

        nodeService = null;
    }

    @Override
    public void NodeServiceEvent(NodeServiceEvent ev, NodeServiceEventData evData) {
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

            case NODE_EXTRACTING:
                Log.d(TAG, "Received NODE_EXTRACTING");
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
