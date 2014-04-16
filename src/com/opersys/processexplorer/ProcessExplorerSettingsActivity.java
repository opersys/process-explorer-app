package com.opersys.processexplorer;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import com.opersys.processexplorer.node.NodeThreadEvent;
import com.opersys.processexplorer.node.NodeThreadEventData;
import com.opersys.processexplorer.node.NodeThreadListener;
import com.opersys.processexplorer.tasks.AssetExtractTask;
import com.opersys.processexplorer.tasks.AssetExtractTaskParams;
import com.opersys.processexplorer.tasks.LocalIPAddressTask;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;

public class ProcessExplorerSettingsActivity extends PreferenceActivity
        implements NodeThreadListener {

    public static final String TAG = "ProcessExplorer";

    protected ProcessExplorerServiceBinder serviceBinder;
    protected ProcessExplorerServiceConnection servConn;

    protected void prepareLayout() {
        final SharedPreferences sharedPrefs;

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

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

                findPreference("browseNow").setSummary("Not started");
                findPreference("browseNow").setEnabled(false);

                return true;
            }
        });

        findPreference("browseNow").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Uri uri;
                Uri.Builder uriBuilder;
                int port;

                uriBuilder = new Uri.Builder();
                port = Integer.parseInt(sharedPrefs.getString("nodePort", "3000"));
                uri = uriBuilder
                        .scheme("http")
                        .encodedAuthority("localhost:" + port)
                        .path("index.html")
                        .build();

                Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(browserIntent);

                return true;
            }
        });
    }

    protected void checkExtractTask() {
        final ProgressDialog progDialog;
        AssetExtractTaskParams extractTaskParams;
        AssetExtractTask extractTask;

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
            progDialog.setCancelable(false);
            progDialog.setCanceledOnTouchOutside(false);

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
        super.onCreate(savedInstanceState);

        prepareLayout();
        checkExtractTask();
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

        if (serviceBinder.isNodeProcessRunning())
            updateOnServiceStarted();
    }

    @Override
    public void onProcessServiceDisconnected() {
        Log.d(TAG, "Disconnected from Node service");

        serviceBinder = null;
    }

    /**
     * All the actions that are to be executed when the service stops or is stopped
     * when the activity is started.
     */
    protected void updateOnServiceStopped() {
        findPreference("stopNow").setEnabled(false);
        findPreference("startNow").setEnabled(true);
        findPreference("browseNow").setEnabled(false);
    }

    /**
     * All the actions that are to be executed when the service starts or is started
     * when the activity is started.
     */
    protected void updateOnServiceStarted() {
        final SharedPreferences sharedPrefs;

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Enable the Browse Now button..
        new LocalIPAddressTask() {
            @Override
            public void onPostExecute(InetAddress inetAddress) {
                int port = Integer.parseInt(sharedPrefs.getString("nodePort", "3000"));
                URL url;

                try {
                    url = new URL("http", inetAddress.getHostAddress(), port, "");
                    findPreference("browseNow").setSummary("Browse to " + url.toString());
                    findPreference("browseNow").setEnabled(true);

                } catch (MalformedURLException e) {
                    Log.e(TAG, "Cannot form URL", e);
                }
            }
        }.execute();

        findPreference("startNow").setEnabled(false);
        findPreference("stopNow").setEnabled(true);
    }

    @Override
    public void ProcessExplorerServiceEvent(NodeThreadEvent ev, NodeThreadEventData evData) {
        switch (ev) {
            case NODE_STARTED:
                Log.d(TAG, "Received NODE_STARTED");
                updateOnServiceStarted();
                break;

            case NODE_ERROR:
            case NODE_STOPPED:
                Log.d(TAG, "Received NODE_STOPPED");
                updateOnServiceStopped();
                break;
        }
    }
}
