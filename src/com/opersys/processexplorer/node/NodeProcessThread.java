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

    private Handler msgHandler;
    private ProcessExplorerService service;
    private Process nodeProcess;

    private boolean isStopping;

    public void stopProcess() {
        DataOutputStream os;
        Timer tm;

        os = new DataOutputStream(nodeProcess.getOutputStream());

        try {
            os.writeChars("quit\n");
            os.flush();

            nodeProcess.getOutputStream().close();
            isStopping = true;
            tm = new Timer("nodeKill", true);
            tm.schedule(new TimerTask() {
                @Override
                public void run() {
                    Log.w(TAG, "The node process didn't end in a timely manner, destroying it");
                    nodeProcess.destroy();
                }
            }, 5000);

        } catch (IOException e) {
            // If we could not send the quit command to the process, forcefully
            // destroy it. This means we will not be able to read the streams but
            // that is preferrable to having the process stick around.
            Log.w(TAG, "Could not send quite command to process, destroying it.");
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

            nodeProcessBuilder
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

            // Loops through interruption if the interruption was not caused by the
            // process being asked to stop.
            while (!isStopping) {
                try {
                    nodeProcess.waitFor();

                } catch (InterruptedException e) {
                    Log.i(TAG, "Interrupting wait on Node process");
                }
            }

            sin = new StringBuffer();
            serr = new StringBuffer();

            try {
                Log.d(TAG, "Reading error stream");

                if (nodeProcess.getErrorStream() != null) {
                    berr = new BufferedReader(new InputStreamReader(nodeProcess.getErrorStream()));
                    while ((s = berr.readLine()) != null)
                        serr.append(s);
                }
                Log.d(TAG, "Done reading error stream");

            } catch (IOException ex) {
                Log.e(TAG, "Exception reading error output", ex);
            }

            if (nodeProcess.exitValue() == 0) {
                msgHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        service.fireNodeServiceEvent(NodeThreadEvent.NODE_STOPPED,
                                new NodeThreadEventData(sin.toString(), serr.toString()));
                    }
                });
            } else {
                msgHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        service.fireNodeServiceEvent(NodeThreadEvent.NODE_ERROR,
                                new NodeThreadEventData(sin.toString(), serr.toString()));
                    }
                });
            }

        } catch (IOException e) {
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
                             Handler msgHandler,
                             ProcessExplorerService service) {
        this.dir = dir;
        this.msgHandler = msgHandler;
        this.service = service;
        this.exec = dir + "/"+ execfile;
        this.js = dir + "/" + jsfile;

        this.nodeProcessBuilder = new ProcessBuilder();
    }

}