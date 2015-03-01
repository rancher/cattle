package io.cattle.platform.configitem.context.impl;

import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.metadata.service.MetadataService;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class MetadataRedirectInfoFactory extends AbstractAgentBaseContextFactory {

    MetadataService metadataService;

    @Override
    protected void populateContext(Agent agent, Instance agentInstance, ConfigItem item, ArchiveContext context) {
        context.getData().put("metadataRedirects", metadataService.getMetadataRedirects(agent));
    }

    public MetadataService getMetadataService() {
        return metadataService;
    }

    @Inject
    public void setMetadataService(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

}
