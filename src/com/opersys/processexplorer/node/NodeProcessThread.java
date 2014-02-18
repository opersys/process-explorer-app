package com.opersys.processexplorer.node;

import android.content.Context;
import android.os.Handler;

import java.io.*;

/**
 * Simple process listener thread.
 */
public class NodeProcessThread extends Thread {

    private String exec;
    private String js;
    private String dir;

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

    @Override
    public void run() {
        BufferedReader bin, berr;
        final StringBuffer sin, serr;
        String s;

        nodeProcessBuilder = new ProcessBuilder()
                .directory(new File(dir))
                .command(exec, js);

        try {
            nodeProcess = nodeProcessBuilder.start();
            nodeProcess.waitFor();

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

    public NodeProcessThread(String execfile,
                             String jsfile,
                             String workDir,
                             Handler msgHandler,
                             NodeService service) {
        this.msgHandler = msgHandler;
        this.service = service;
        this.exec = execfile;
        this.js = jsfile;
        this.dir = workDir;
    }
}