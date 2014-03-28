package io.cattle.platform.configitem.server.template;

import io.cattle.platform.configitem.server.resource.Resource;

import java.io.IOException;

public interface TemplateLoader {

    public static final int NO_PRIORITY = Integer.MAX_VALUE;
    public static final int HIGH_PRIORITY = 100;
    public static final int MID_PRIORITY = 500;
    public static final int LOW_PRIORITY = 1000;

    int canHandle(Resource resource);

    Template loadTemplate(Resource resource) throws IOException;

}
