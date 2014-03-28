package io.cattle.platform.configitem.server.template.impl;

import io.cattle.platform.configitem.server.resource.Resource;
import io.cattle.platform.configitem.server.template.Template;

import java.io.IOException;

import javax.inject.Inject;

import freemarker.template.Configuration;

public class FreemarkerTemplateLoader extends AbstractExtBasedTemplateLoader {

    Configuration configuration;

    @Override
    protected Template loadTemplate(String outputName, Resource resource) throws IOException {
        freemarker.template.Template template = configuration.getTemplate(resource.getURL().toExternalForm());
        return new FreemarkerTemplate(outputName, template, resource);
    }

    @Override
    public int getPriority() {
        return MID_PRIORITY;
    }

    @Override
    public String getExt() {
        return ".ftl";
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    @Inject
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

}
