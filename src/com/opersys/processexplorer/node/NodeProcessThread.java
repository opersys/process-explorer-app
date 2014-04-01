package com.opersys.processexplorer.node;

import android.os.Handler;
import android.util.Log;
import com.opersys.processexplorer.ProcessExplorerService;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Simple process listener thread.
 */
public class NodeProcessThread extends Thread {

    private static final String TAG = "NodeProcessThread";

    private String dir;
    private String exec;
    private String js;

    private Handler msgHandler;
    private ProcessExplorerService service;
    private Process nodeProcess;
    private ProcessBuilder nodeProcessBuilder;

    private boolean isStopping;

    public void stopProcess() {
        // This asks the process to stop itself.
        isStopping = true;
        this.interrupt();
    }

    public void startProcess() {
        start();
    }

    @Override
    public void run() {
        final StringBuffer sin, serr;
        final NodeThreadEventData emptyEventData;
        BufferedReader bin, berr;
        String s;

        emptyEventData = new NodeThreadEventData();

        try {
            msgHandler.post(new Runnable() {
                @Override
                public void run() {
                    service.fireNodeServiceEvent(NodeThreadEvent.NODE_STARTING, emptyEventData);
                }
            });

            nodeProcessBuilder = new ProcessBuilder()
                    .directory(new File(dir))
                    .command(exec, js);

            if (!isStopping) {
                nodeProcess = nodeProcessBuilder.start();

                msgHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        service.fireNodeServiceEvent(NodeThreadEvent.NODE_STARTED, emptyEventData);
                    }
                });
            }

            while (!isStopping) {
                try {
                    nodeProcess.waitFor();
                } catch (InterruptedException e) {
                    Log.i(TAG, "Interrupting wait on Node process");
                }
            }

            nodeProcess.destroy();

            sin = new StringBuffer();
            serr = new StringBuffer();

            // Read the outputs
            if (nodeProcess.getInputStream() != null) {
                bin = new BufferedReader(new InputStreamReader(nodeProcess.getInputStream()));
                while ((s = bin.readLine()) != null)
                    sin.append(s);
            }
            if (nodeProcess.getErrorStream() != null) {
                berr = new BufferedReader(new InputStreamReader(nodeProcess.getErrorStream()));
                while ((s = berr.readLine()) != null)
                    serr.append(s);
            }

            msgHandler.post(new Runnable() {
                @Override
                public void run() {
                    service.fireNodeServiceEvent(NodeThreadEvent.NODE_STOPPED,
                            new NodeThreadEventData(sin.toString(), serr.toString()));
                }
            });

        } catch (IOException e) {
            msgHandler.post(new Runnable() {
                @Override
                public void run() {
                    service.fireNodeServiceEvent(NodeThreadEvent.NODE_ERROR, emptyEventData);
                }
            });

        } finally {
            nodeProcess.destroy();
            nodeProcess = null;
        }
    }

    public NodeProcessThread(String dir,
                             String execfile,
                             String jsfile,
                             Handler msgHandler,
                             ProcessExplorerService service) {
        this.dir = dir;
        this.msgHandler = msgHandler;
        this.service = service;
        this.exec = dir + "/"+ execfile;
        this.js = dir + "/" + jsfile;
    }

}