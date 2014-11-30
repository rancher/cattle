package io.cattle.platform.configitem.server.agentinclude.impl;

import io.cattle.platform.configitem.context.ConfigItemContextFactory;
import io.cattle.platform.configitem.server.agentinclude.AgentIncludeMap;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.Request;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;

public class AgentPackagesContextFactory implements ConfigItemContextFactory {

    String name;
    AgentIncludeMap map;

    public AgentPackagesContextFactory(String name, AgentIncludeMap map) {
        super();
        this.name = name;
        this.map = map;
    }

    @Override
    public String[] getItems() {
        return new String[] {};
    }

    @Override
    public void populateContext(Request req, ConfigItem item, ArchiveContext context) {
        context.getData().put("data", map.getMap(name));
    }

}
