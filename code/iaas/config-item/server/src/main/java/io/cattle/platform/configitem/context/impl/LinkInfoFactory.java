package io.cattle.platform.configitem.context.impl;

import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.Request;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;

public class LinkInfoFactory extends AbstractAgentBaseContextFactory {

    @Override
    protected void populateContext(Agent agent, Instance instance, ConfigItem item, ArchiveContext context, Request configRequest) {
        // This class now does nothing.  It only here because deleting @Named classes causes latest to fail
        // on update.  This can be deleted in the next release v0.23
    }

}
