package com.opersys.processexplorer.platforminfo;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import com.opersys.processexplorer.ProcessExplorerSettingsActivity;

public class PlatformInfoServiceConnection implements ServiceConnection {
    public PlatformInfoServiceConnection(ProcessExplorerSettingsActivity processExplorerMain) {
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }
}
