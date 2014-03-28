package com.opersys.processexplorer.tasks;

import android.os.AsyncTask;

import java.net.InetAddress;
import java.net.UnknownHostException;

public abstract class LocalIPAddressTask extends AsyncTask<Void, Void, InetAddress> {

    @Override
    protected InetAddress doInBackground(Void... params) {
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            return null;
        }
    }

    @Override
    public abstract void onPostExecute(InetAddress inetAddress);
}
