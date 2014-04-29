package io.cattle.platform.configitem.server.template;

import io.cattle.platform.configitem.context.ConfigItemContextFactory;
import io.cattle.platform.configitem.server.model.impl.AbstractArchiveBasedConfigItem;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.configitem.server.resource.Resource;
import io.cattle.platform.configitem.server.resource.ResourceRoot;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class TemplatesBasedArchiveItem extends AbstractArchiveBasedConfigItem {

    private static final String TEMPLATE_KEY = "template";

    TemplateFactory templateFactory;

    public TemplatesBasedArchiveItem(String name, ConfigItemStatusManager versionManager, ResourceRoot resourceRoot,
            TemplateFactory templateFactory, List<ConfigItemContextFactory> contextFactories) {
        super(name, versionManager, resourceRoot, contextFactories);
        this.templateFactory = templateFactory;
    }

    @Override
    protected void writeContent(final ArchiveContext context) throws IOException {
        super.writeContent(context);

        for ( Resource resource : getResourceRoot().getResources() ) {
            Template template = null;
            Object cached = resource.getAttibute(TEMPLATE_KEY);

            if ( cached == null || ! ( cached instanceof Template )) {
                template = templateFactory.loadTemplate(resource);
                resource.setAttribute(TEMPLATE_KEY, template);
            } else {
                template = (Template)cached;
            }

            if ( template == null )
                continue;

            final Template templateFinal = template;
            withEntry(context, templateFinal.getOutputName(), template.getSize(), new WithEntry() {
                @Override
                public void with(OutputStream os) throws IOException {
                    templateFinal.execute(context.getData(), os);
                }
            });
        }
    }

}
