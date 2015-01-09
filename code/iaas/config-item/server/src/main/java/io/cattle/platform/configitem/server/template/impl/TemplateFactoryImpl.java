package io.cattle.platform.configitem.server.template.impl;

import io.cattle.platform.configitem.server.resource.Resource;
import io.cattle.platform.configitem.server.template.Template;
import io.cattle.platform.configitem.server.template.TemplateFactory;
import io.cattle.platform.configitem.server.template.TemplateLoader;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

public class TemplateFactoryImpl implements TemplateFactory {

    List<TemplateLoader> templateLoaders;

    @Override
    public Template loadTemplate(Resource resource) throws IOException {
        int bestMatch = TemplateLoader.NO_PRIORITY;
        TemplateLoader bestLoader = null;

        for (TemplateLoader loader : templateLoaders) {
            int result = loader.canHandle(resource);
            if (result < bestMatch) {
                bestMatch = result;
                bestLoader = loader;
            }
        }

        return bestLoader == null ? null : bestLoader.loadTemplate(resource);
    }

    public List<TemplateLoader> getTemplateLoaders() {
        return templateLoaders;
    }

    @Inject
    public void setTemplateLoaders(List<TemplateLoader> templateLoaders) {
        this.templateLoaders = templateLoaders;
    }

}
