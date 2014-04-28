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

package com.opersys.processexplorer.tasks;

import android.os.AsyncTask;
import android.util.Log;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public abstract class LocalIPAddressTask extends AsyncTask<Void, Void, InetAddress> {

    private static final String TAG = "LocalIPAddressTask";

    @Override
    protected InetAddress doInBackground(Void... params) {
        try {
            NetworkInterface intf;
            InetAddress inetAddress;

            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
                 en.hasMoreElements() ;) {

                intf = en.nextElement();

                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
                     enumIpAddr.hasMoreElements();) {

                    inetAddress = enumIpAddr.nextElement();

                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address)
                        return inetAddress;
                }
            }
        } catch (SocketException ex) {
            Log.e(TAG, ex.toString());
        }

        return null;
    }

    @Override
    public abstract void onPostExecute(InetAddress inetAddress);
}
