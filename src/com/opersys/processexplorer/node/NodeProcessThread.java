package com.opersys.processexplorer.node;

import android.content.res.AssetManager;
import android.os.Handler;
import android.util.Log;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;

/**
 * Simple process listener thread.
 */
public class NodeProcessThread extends Thread {

    private static final String TAG = "ProcessExplorer-NodeProcessThread";

    private AssetManager assetManager;
    private String dir;
    private String exec;
    private String js;

    private Handler msgHandler;
    private NodeService service;
    private Process nodeProcess;
    private ProcessBuilder nodeProcessBuilder;

    public void startProcess() {
        this.start();
    }

    public void endProcess() {
        if (nodeProcess != null)
            nodeProcess.destroy();

        nodeProcessBuilder = null;
        nodeProcess = null;
    }

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

    public void extractAsset() {
        InputStream is;
        GzipCompressorInputStream gzis;
        TarArchiveInputStream tgzis;
        TarArchiveEntry tentry;

        try {
            is = assetManager.open("system-explorer.tgz");
            gzis = new GzipCompressorInputStream(is);
            tgzis = new TarArchiveInputStream(gzis);

            while ((tentry = tgzis.getNextTarEntry()) != null) {
                final File outputTarget = new File(dir, tentry.getName());

                if (tentry.isDirectory()) {
                    if (!outputTarget.exists()) {
                        if (!outputTarget.mkdirs()) {
                            String s = String.format("Couldn't create directory %s.", outputTarget.getAbsolutePath());
                            throw new IllegalStateException(s);
                        }
                    }
                } else {
                    final File parentTarget = new File(outputTarget.getParent());

                    // Make the parent directory if it doesn't exists.
                    if (!parentTarget.exists())
                    {
                        if (!parentTarget.mkdirs()) {
                            String s = String.format("Couldn't create directory %s.", parentTarget.toString());
                            throw new IllegalStateException(s);
                        }
                    }

                    final OutputStream outputFileStream = new FileOutputStream(outputTarget);
                    IOUtils.copy(tgzis, outputFileStream);
                    outputFileStream.close();
                }
            }
        } catch (IOException ex) {
            Log.e(TAG, "Asset decompression error", ex);
        }

        try {
            setMode("0777", dir + "/node");
        } catch (IOException e) {
            Log.e(TAG, "Failed to made node binary executable", e);
        }

    }

    @Override
    public void run() {
        BufferedReader bin, berr;
        final StringBuffer sin, serr;
        String s;

        Log.d(TAG, "Node process thread starting");

        nodeProcessBuilder = new ProcessBuilder()
                .directory(new File(dir))
                .command(exec, js);

        extractAsset();

        try {
            Log.d(TAG, "Node process thread started");

            nodeProcess = nodeProcessBuilder.start();
            nodeProcess.waitFor();

            Log.d(TAG, "Node process thread stopping");

            // Read the outputs
            bin = new BufferedReader(new InputStreamReader(nodeProcess.getInputStream()));
            berr = new BufferedReader(new InputStreamReader(nodeProcess.getErrorStream()));
            sin = new StringBuffer();
            serr = new StringBuffer();

            while ((s = bin.readLine()) != null) sin.append(s);
            while ((s = berr.readLine()) != null) serr.append(s);

            msgHandler.post(new Runnable() {
                @Override
                public void run() {
                    service.fireNodeServiceEvent(
                            NodeServiceEvent.NODE_QUIT,
                            new NodeServiceEventData(sin.toString(), serr.toString()));
                }
            });

        } catch (IOException e) {
            endProcess();

            msgHandler.post(new Runnable() {
                @Override
                public void run() {
                    service.fireNodeServiceEvent(
                            NodeServiceEvent.NODE_ERROR,
                            new NodeServiceEventData());
                }
            });

        } catch (InterruptedException e) {
            endProcess();

            msgHandler.post(new Runnable() {
                @Override
                public void run() {
                    service.fireNodeServiceEvent(
                            NodeServiceEvent.NODE_ERROR,
                            new NodeServiceEventData());
                }
            });
        }
    }

    public NodeProcessThread(AssetManager assetManager,
                             String dir,
                             String execfile,
                             String jsfile,
                             Handler msgHandler,
                             NodeService service) {
        this.assetManager = assetManager;
        this.dir = dir;
        this.msgHandler = msgHandler;
        this.service = service;
        this.exec = dir + "/"+ execfile;
        this.js = dir + "/" + jsfile;
    }
}