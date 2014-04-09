package com.opersys.processexplorer.platforminfo;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import org.restlet.data.CacheDirective;
import org.restlet.data.MediaType;
import org.restlet.representation.OutputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StreamRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Date: 04/04/14
 * Time: 11:31 AM
 */
public class IconResource extends ServerResource {

    private static final String TAG = "IconResource";
    protected PackageManager pm;

    @Get
    public Representation doGet() {
        final BitmapDrawable drawable;
        final ByteArrayOutputStream imgOut;
        String app;
        PackageManager pm;
        StreamRepresentation sr = null;

        pm = (PackageManager) getContext().getAttributes().get("pm");

        // Make sure this is cached for a little while.
        getResponse().getCacheDirectives().add(
                CacheDirective.maxAge(3600 * 24)
        );

        app = (String) getRequest().getAttributes().get("app");

        try {
            drawable = (BitmapDrawable) pm.getApplicationIcon(app);
            imgOut = new ByteArrayOutputStream();
            drawable.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, imgOut);

            sr = new OutputRepresentation(MediaType.IMAGE_PNG, imgOut.size()) {
                @Override
                public void write(OutputStream outputStream) throws IOException {
                    outputStream.write(imgOut.toByteArray());
                }
            };

        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package not found: " + pm);
        }

        return sr;
    }
}
