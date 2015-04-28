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

package com.opersys.processexplorer.node;

import android.os.Handler;
import android.util.Log;
import com.opersys.processexplorer.ProcessExplorerService;

import java.io.*;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Author: François-Denis Gonthier (francois-denis.gonthier@opersys.com)
 *
 * Simple process listener thread.
 */
public class NodeProcessThread extends Thread {

    private static final String TAG = "NodeProcessThread";
    private ProcessBuilder nodeProcessBuilder;

    private String dir;
    private String exec;
    private String js;
    private String suExec;
    private boolean asRoot;

    private Handler msgHandler;
    private ProcessExplorerService service;
    private Process nodeProcess;

    private Timer tm;

    public void stopProcess() {
        // Prepare a forcekill timer *just in case*...
        tm.schedule(new TimerTask() {
            @Override
            public void run() {
            Log.w(TAG, "The node process didn't end in a timely manner, destroying it");
            nodeProcess.destroy();
            }
        }, 5000);

        try {
            OutputStream stdin = nodeProcess.getOutputStream();
            stdin.write("quit\n".getBytes());
            stdin.flush();
            stdin.close();
        } catch (IOException e) {
            // If we could not send the quit command to the process, forcefully
            // destroy it now.
            Log.w(TAG, "Could not send quit command to process, destroying it.");
            nodeProcess.destroy();
        }
    }

    public void startProcess() {
        start();
    }

    public void setEnvironment(String varName, String varValue) {
        nodeProcessBuilder
                .environment()
                .put(varName, varValue);
    }

    @Override
    public void run() {
        final StringBuffer sout, serr;
        final NodeThreadEventData emptyEventData;
        BufferedReader processOutput;
        String s;

        emptyEventData = new NodeThreadEventData();

        try {
            msgHandler.post(new Runnable() {
                @Override
                public void run() {
                    service.fireNodeServiceEvent(NodeThreadEvent.NODE_STARTING, emptyEventData);
                }
            });

            if (asRoot && suExec != null) {
                nodeProcessBuilder
                    .directory(new File(dir))
                    .command(suExec, "-c", "cd " + dir + " && " + exec + " " + js);
            }
            else {
                nodeProcessBuilder
                    .directory(new File(dir))
                    .command(exec, js);
            }

            nodeProcess = nodeProcessBuilder.start();

            msgHandler.post(new Runnable() {
                @Override
                public void run() {
                    service.fireNodeServiceEvent(NodeThreadEvent.NODE_STARTED, emptyEventData);
                }
            });

            // Wait for the process to quit
            try {
                nodeProcess.waitFor();
            } catch (InterruptedException e) {
                Log.i(TAG, "Interrupting wait on Node process");
            }

            // Successful (nor not) quit, cancel all the things
            tm.cancel();

            // nodejs stdout / stderr
            sout = new StringBuffer();
            serr = new StringBuffer();

            try {
                Log.d(TAG, "Reading process stdout...");
                if (nodeProcess.getInputStream() != null) {
                    processOutput = new BufferedReader(new InputStreamReader(nodeProcess.getInputStream()));

                    while ((s = processOutput.readLine()) != null) {
                        serr.append(s);
                    }
                }

                Log.d(TAG, "Reading process stderr...");
                if (nodeProcess.getErrorStream() != null) {
                    processOutput = new BufferedReader(new InputStreamReader(nodeProcess.getErrorStream()));

                    while ((s = processOutput.readLine()) != null) {
                        serr.append(s);
                    }
                }

                Log.d(TAG, "Done reading stdout/stderr.");

            } catch (IOException ex) {
                Log.e(TAG, "Exception while reading stdout/stderr!", ex);
            }

            if (nodeProcess.exitValue() == 0) {
                // Process exited as we asked for
                msgHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        service.fireNodeServiceEvent(NodeThreadEvent.NODE_STOPPED,
                                new NodeThreadEventData(sout.toString(), serr.toString()));
                    }
                });
            } else {
                // Process couldn't exit successfully
                msgHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        service.fireNodeServiceEvent(NodeThreadEvent.NODE_ERROR,
                                new NodeThreadEventData(sout.toString(), serr.toString()));
                    }
                });
            }
        } catch (IOException e) {
            // An error launching node process?
            final NodeThreadEventData evData = new NodeThreadEventData(e);

            msgHandler.post(new Runnable() {
                @Override
                public void run() {
                    service.fireNodeServiceEvent(NodeThreadEvent.NODE_ERROR, evData);
                }
            });

        } finally {
            // Make sure everything about the process is destroyed.
            nodeProcess.destroy();
            nodeProcess = null;
        }
    }

    public NodeProcessThread(String dir,
                             String execfile,
                             String jsfile,
                             boolean asRoot,
                             Handler msgHandler,
                             ProcessExplorerService service) {
        String[] suFiles = { "/system/xbin/su", "/system/bin/su" };

        for (String sf : suFiles) {
            if (new File(sf).exists()) {
                this.suExec = sf;
            }
        }

        this.dir = dir;
        this.msgHandler = msgHandler;
        this.service = service;
        this.exec = dir + "/"+ execfile;
        this.js = dir + "/" + jsfile;
        this.exec = dir + "/"+ execfile;
        this.asRoot = asRoot;

        this.nodeProcessBuilder = new ProcessBuilder();

        // A forcekill timer used in case nodeprocess becomes unresponsive
        this.tm = new Timer("nodeKill", true);
    }
}
