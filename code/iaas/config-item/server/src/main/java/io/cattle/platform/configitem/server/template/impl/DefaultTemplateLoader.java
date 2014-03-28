package io.cattle.platform.configitem.server.template.impl;

import io.cattle.platform.configitem.server.resource.Resource;
import io.cattle.platform.configitem.server.template.Template;
import io.cattle.platform.configitem.server.template.TemplateLoader;

public class DefaultTemplateLoader implements TemplateLoader {

    @Override
    public Template loadTemplate(Resource resource) {
        return new RawResourceTemplate(resource);
    }

    @Override
    public int canHandle(Resource resource) {
        return LOW_PRIORITY;
    }
}
