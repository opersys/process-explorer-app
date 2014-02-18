package com.opersys.processexplorer.node;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

public class NodeService extends Service {

    public class NodeServiceBinder extends Binder {
        public NodeService getService() {
            return NodeService.this;
        }
    }

    class NodeProcessHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }

    private static final String TAG = "NodeService";

    private NodeProcessThread nodeThread;

    private NodeServiceBinder peBinder;

    private List<NodeServiceListener> nodeServiceListeners;

    private void setMode(String modeStr, String path) throws IOException {
        ProcessBuilder chmodProcBuilder = new ProcessBuilder();
        Process chmodProc;

        chmodProcBuilder.command("chmod", modeStr, path);
        chmodProc = chmodProcBuilder.start();

        try {
            chmodProc.waitFor();
        } catch (InterruptedException e) {
            // FIXME: Not sure what to do here.
        }
    }

    private void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;

        while((read = in.read(buffer)) != -1)
            out.write(buffer, 0, read);
    }

    private void copyAsset(String assetPath, String targetPath) {
        AssetManager assetManager;
        FileOutputStream os;
        InputStream is;
        byte[] buff = new byte[4096];

        try {
            assetManager = getAssets();
            is = assetManager.open(assetPath);
            os = openFileOutput(targetPath, MODE_PRIVATE);

            copyStream(is, os);

            os.close();
            is.close();

        } catch (FileNotFoundException e) {
            Log.w(TAG, "Failed to open asset", e);
        } catch (IOException e) {
            Log.w(TAG, "Error loading the assets", e);
        }
    }

    protected void fireNodeServiceEvent(NodeServiceEvent ev, NodeServiceEventData evData) {
        for (NodeServiceListener nsListener : nodeServiceListeners)
            nsListener.NodeServiceEvent(ev, evData);
    }

    public void addNodeServiceListener(NodeServiceListener nsListener) {
        nodeServiceListeners.add(nsListener);
    }

    public void removeNodeServiceListener(NodeServiceListener nsListener) {
        nodeServiceListeners.remove(nsListener);
    }

    public void start() {
        AssetManager assetManager;
        InputStream is;
        GzipCompressorInputStream gzis;
        TarArchiveInputStream tgzis;
        TarArchiveEntry tentry;

        Log.i(TAG, "Node Service started");

        try {
            assetManager = getAssets();
            is = assetManager.open("system-explorer.tgz");
            gzis = new GzipCompressorInputStream(is);
            tgzis = new TarArchiveInputStream(gzis);

            while ((tentry = tgzis.getNextTarEntry()) != null) {
                final File outputFile = new File(getFilesDir(), tentry.getName());
                if (tentry.isDirectory()) {
                    if (!outputFile.exists()) {
                        if (!outputFile.mkdirs()) {
                            throw new IllegalStateException(String.format("Couldn't create directory %s.", outputFile.getAbsolutePath()));
                        }
                    }
                } else {
                    final OutputStream outputFileStream = new FileOutputStream(outputFile);
                    IOUtils.copy(tgzis, outputFileStream);
                    outputFileStream.close();
                }
            }
        } catch (IOException ex) {

        }

        try {
            setMode("0777", getFilesDir() + "/node");
        } catch (IOException e) {
            Log.e(TAG, "Failed to made node binary executable", e);
        }

        nodeThread = new NodeProcessThread(
                getFilesDir().toString() + "/node",
                getFilesDir().toString() + "/HelloWorld.js",
                getFilesDir().toString(),
                new NodeProcessHandler(),
                this);

        nodeThread.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (nodeThread != null) {
            nodeThread.endProcess();
            nodeThread = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return peBinder;
    }

    public NodeService() {
        super();

        peBinder = new NodeServiceBinder();
        nodeServiceListeners = new LinkedList<NodeServiceListener>();
    }
}
