package com.opersys.processexplorer.platforminfo;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

/**
 * Date: 31/03/14
 * Time: 12:16 PM
 */
public class PlatformInfoServiceResource extends ServerResource {

    @Get
    public String toString() {
        return "hello, world";
    }
}
