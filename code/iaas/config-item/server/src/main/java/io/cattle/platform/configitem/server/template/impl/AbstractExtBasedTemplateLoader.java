package io.cattle.platform.configitem.server.template.impl;

import io.cattle.platform.configitem.server.resource.Resource;
import io.cattle.platform.configitem.server.template.Template;
import io.cattle.platform.configitem.server.template.TemplateLoader;

import java.io.IOException;

public abstract class AbstractExtBasedTemplateLoader implements TemplateLoader {

    @Override
    public int canHandle(Resource resource) {
        if (resource.getName().endsWith(getExt())) {
            return getPriority();
        }
        return TemplateLoader.NO_PRIORITY;
    }

    @Override
    public Template loadTemplate(Resource resource) throws IOException {
        String name = resource.getName();
        String outputName = name.substring(0, name.length() - getExt().length());
        return loadTemplate(outputName, resource);
    }

    protected abstract Template loadTemplate(String outputName, Resource resource) throws IOException;

    public abstract int getPriority();

    public abstract String getExt();
}
