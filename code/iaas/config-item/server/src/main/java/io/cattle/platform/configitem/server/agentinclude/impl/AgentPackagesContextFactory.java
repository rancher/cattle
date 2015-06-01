package io.cattle.platform.configitem.server.agentinclude.impl;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.configitem.context.impl.AbstractAgentBaseContextFactory;
import io.cattle.platform.configitem.model.Client;
import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.configitem.server.agentinclude.AgentIncludeMap;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.object.ObjectManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicListProperty;

public class AgentPackagesContextFactory extends AbstractAgentBaseContextFactory {

    public static final DynamicListProperty<String> REQUIRED = ArchaiusUtil.getList("agent.config.items");

    private static final Logger log = LoggerFactory.getLogger(AgentPackagesContextFactory.class);

    ConfigItemStatusManager statusManager;
    String name;
    AgentIncludeMap map;

    public AgentPackagesContextFactory(String name, AgentIncludeMap map, ObjectManager objectManager, ConfigItemStatusManager statusManager) {
        super();
        this.name = name;
        this.map = map;
        this.objectManager = objectManager;
        this.statusManager = statusManager;
    }

    @Override
    public String getContentHash(String hash) {
        return hash + map.getSourceRevision(name);
    }

    @Override
    public String[] getItems() {
        return new String[] {};
    }

    @Override
    protected void populateContext(Agent agent, Instance instance, ConfigItem item, ArchiveContext context) {
        context.getData().put("data", map.getMap(name));

        if (instance != null) {
            return;
        }

        Client client = new Client(Agent.class, agent.getId());
        ConfigUpdateRequest request = new ConfigUpdateRequest(client).withDeferredTrigger(true);

        for (String itemName : REQUIRED.get()) {
            if (!statusManager.isAssigned(client, itemName)) {
                log.info("Adding missing [{}] to agent [{}]", itemName, agent.getId());
                request.addItem(itemName);
            }
        }

        if (request.getItems().size() > 0) {
            statusManager.updateConfig(request);
        }
    }

}
