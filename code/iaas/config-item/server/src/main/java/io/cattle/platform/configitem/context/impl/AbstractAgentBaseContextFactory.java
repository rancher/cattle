package io.cattle.platform.configitem.context.impl;

import static io.cattle.platform.core.model.tables.InstanceTable.*;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.configitem.context.ConfigItemContextFactory;
import io.cattle.platform.configitem.model.Client;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.Request;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.object.ObjectManager;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractAgentBaseContextFactory implements ConfigItemContextFactory {

    private static final Logger log = LoggerFactory.getLogger(AbstractAgentBaseContextFactory.class);

    protected ObjectManager objectManager;

    String[] items;

    public AbstractAgentBaseContextFactory() {
        String name = getClass().getSimpleName().replaceAll("Factory", "").replaceAll("([a-z])([A-Z])", "$1.$2").toLowerCase();
        List<String> items = ArchaiusUtil.getList(String.format("item.context.%s.items", name)).get();
        this.items = items.toArray(new String[items.size()]);
    }

    @Override
    public String[] getItems() {
        return items;
    }

    @Override
    public String getContentHash(String hash) {
        return hash;
    }

    @Override
    public final void populateContext(Request req, ConfigItem item, ArchiveContext context) {
        Client client = req.getClient();
        if (!Agent.class.equals(client.getResourceType())) {
            return;
        }

        Agent agent = objectManager.loadResource(Agent.class, client.getResourceId());
        if (agent == null) {
            return;
        }

        List<Instance> instances = objectManager.find(Instance.class, INSTANCE.AGENT_ID, agent.getId(), INSTANCE.REMOVED, null);

        if (instances.size() > 1) {
            List<Long> ids = new ArrayList<Long>();
            for (Instance instance : instances) {
                ids.add(instance.getId());
            }

            log.error("Found more that one instance for Agent [{}], instances {}", agent.getId(), ids);
        }

        populateContext(agent, instances.size() > 0 ? instances.get(0) : null, item, context, req);
    }

    protected abstract void populateContext(Agent agent, Instance instance, ConfigItem item, ArchiveContext context, Request configRequest);

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

}