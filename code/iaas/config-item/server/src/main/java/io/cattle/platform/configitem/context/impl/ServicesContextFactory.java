package io.cattle.platform.configitem.context.impl;

import javax.inject.Named;

import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.ConfigItemStatus;
import io.cattle.platform.core.model.Instance;

@Named
public class ServicesContextFactory extends AbstractAgentBaseContextFactory {

    @Override
    public String[] getItems() {
        return new String[] { "services" };
    }

    @Override
    protected void populateContext(Agent agent, Instance instance, ConfigItem item, ArchiveContext context) {
        context.getData().put("configItemStatuses", objectManager.children(agent, ConfigItemStatus.class));
    }

}
