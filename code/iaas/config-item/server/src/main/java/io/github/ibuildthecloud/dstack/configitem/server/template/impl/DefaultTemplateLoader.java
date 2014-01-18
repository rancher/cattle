package io.github.ibuildthecloud.dstack.configitem.server.template.impl;

import io.github.ibuildthecloud.dstack.configitem.server.resource.Resource;
import io.github.ibuildthecloud.dstack.configitem.server.template.Template;
import io.github.ibuildthecloud.dstack.configitem.server.template.TemplateLoader;

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
