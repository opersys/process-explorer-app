package com.opersys.processexplorer.platforminfo;

import android.util.Log;
import org.restlet.Server;
import org.restlet.data.Protocol;

public class PlatformInfoServer {

    private static String TAG = "PlatformInfoServer";

    protected Server restServer;

    public void startServer() {
        restServer = new Server(Protocol.HTTP, 3001, PlatformInfoServiceResource.class);

        try {
            restServer.start();

            Log.i(TAG, "Platform information restlet was started.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start platform information restlet.", e);
        }
    }

    public void stopServer() {
        if (restServer != null) {
            try {
                restServer.stop();
            } catch (Exception e) {
                Log.e(TAG, "Failed to stop platform information restlet", e);
            }
        }
    }
}
