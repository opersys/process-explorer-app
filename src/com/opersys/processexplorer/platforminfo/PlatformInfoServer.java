/*
 Copyright 2014 Opersys inc.

 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
*/

package com.opersys.processexplorer.platforminfo;

import android.content.pm.PackageManager;
import android.util.Log;
import org.restlet.Component;
import org.restlet.data.Protocol;

public class PlatformInfoServer {

    private static String TAG = "PlatformInfoServer";

    protected Component mainComp;

    public void startServer() {
        try {
            mainComp.start();
            Log.i(TAG, "Platform information server started");
        } catch (Exception e) {
            Log.w(TAG, "Platform information server failed to start", e);
        }
    }

    public void stopServer() {
        try {
            mainComp.stop();
            Log.i(TAG, "Platform information server stopped");
        } catch (Exception e) {
            Log.w(TAG, "Platform information server failed to stop", e);
        }
    }

    public PlatformInfoServer(PackageManager pm) {
        mainComp = new Component();

        // Servers
        mainComp.getServers().add(Protocol.HTTP, 3001);
        mainComp.getDefaultHost().attachDefault(new PlatformInfoApp(pm));
    }
}
