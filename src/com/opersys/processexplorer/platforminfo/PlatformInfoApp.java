package com.opersys.processexplorer.platforminfo;

import android.content.pm.PackageManager;
import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

/**
 * Date: 04/04/14
 * Time: 10:54 PM
 */
public class PlatformInfoApp extends Application {

    protected Context appCtx;

    @Override
    public Restlet createInboundRoot() {
        Router router = new Router();

        router.setContext(appCtx);

        // Routes
        router.attach("/icon/", IconResource.class);
        router.attach("/icon/{app}", IconResource.class);
        router.attachDefault(DefaultResource.class);

        return router;
    }

    public PlatformInfoApp(PackageManager pm) {
        appCtx = new Context();
        appCtx.getAttributes().put("pm", pm);
    }
}
