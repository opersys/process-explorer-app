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
