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
