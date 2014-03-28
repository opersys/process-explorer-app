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
        //nodeProcess = null;
    }

    @Override
    public void run() {
        BufferedReader bin, berr;
        final StringBuffer sin, serr;
        String s;

        try {
            msgHandler.post(new Runnable() {
                @Override
                public void run() {
                    service.fireNodeServiceEvent(NodeServiceEvent.NODE_STARTING, new NodeServiceEventData());
                }
            });

            nodeProcessBuilder = new ProcessBuilder()
                    .directory(new File(dir))
                    .command(exec, js);
            nodeProcess = nodeProcessBuilder.start();

            msgHandler.post(new Runnable() {
                @Override
                public void run() {
                    service.fireNodeServiceEvent(NodeServiceEvent.NODE_STARTED, new NodeServiceEventData());
                }
            });

            nodeProcess.waitFor();

            sin = new StringBuffer();
            serr = new StringBuffer();

            // Read the outputs
            if (nodeProcess.getInputStream() != null) {
                bin = new BufferedReader(new InputStreamReader(nodeProcess.getInputStream()));
                while ((s = bin.readLine()) != null) sin.append(s);
            }
            if (nodeProcess.getErrorStream() != null) {
                berr = new BufferedReader(new InputStreamReader(nodeProcess.getErrorStream()));
                while ((s = berr.readLine()) != null) serr.append(s);
            }

            msgHandler.post(new Runnable() {
                @Override
                public void run() {
                    service.fireNodeServiceEvent(NodeServiceEvent.NODE_STOPPED, new NodeServiceEventData());
                }
            });

        } catch (IOException e) {
            msgHandler.post(new Runnable() {
                @Override
                public void run() {
                    service.fireNodeServiceEvent(NodeServiceEvent.NODE_ERROR, new NodeServiceEventData());
                }
            });

        } catch (InterruptedException e) {
            msgHandler.post(new Runnable() {
                @Override
                public void run() {
                    service.fireNodeServiceEvent(NodeServiceEvent.NODE_ERROR, new NodeServiceEventData());
                }
            });

        } finally {
            endProcess();
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