package com.opersys.processexplorer.platforminfo;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

/**
 * Date: 04/04/14
 * Time: 11:57 AM
 */
public class DefaultResource extends ServerResource {

    @Get
    public String toString() {
        return "Platform Information Restlet";
    }
}
