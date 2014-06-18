package io.cattle.platform.configitem.context.impl;

import io.cattle.platform.configitem.context.dao.MetadataDao;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class MetadataRedirectInfoFactory extends AbstractAgentBaseContextFactory {

    MetadataDao metadataDao;

    @Override
    protected void populateContext(Agent agent, Instance agentInstance, ConfigItem item, ArchiveContext context) {
        context.getData().put("metadataRedirects", metadataDao.getMetadataRedirects(agent));
    }

    public MetadataDao getMetadataDao() {
        return metadataDao;
    }

    @Inject
    public void setMetadataDao(MetadataDao metadataDao) {
        this.metadataDao = metadataDao;
    }

}
