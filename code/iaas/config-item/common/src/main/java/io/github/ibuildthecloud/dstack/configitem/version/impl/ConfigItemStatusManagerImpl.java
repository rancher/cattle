package io.github.ibuildthecloud.dstack.configitem.version.impl;

import io.github.ibuildthecloud.dstack.agent.AgentLocator;
import io.github.ibuildthecloud.dstack.agent.RemoteAgent;
import io.github.ibuildthecloud.dstack.async.utils.AsyncUtils;
import io.github.ibuildthecloud.dstack.configitem.events.ConfigUpdate;
import io.github.ibuildthecloud.dstack.configitem.exception.ConfigTimeoutException;
import io.github.ibuildthecloud.dstack.configitem.model.Client;
import io.github.ibuildthecloud.dstack.configitem.model.ItemVersion;
import io.github.ibuildthecloud.dstack.configitem.model.impl.DefaultClient;
import io.github.ibuildthecloud.dstack.configitem.request.ConfigUpdateItem;
import io.github.ibuildthecloud.dstack.configitem.request.ConfigUpdateRequest;
import io.github.ibuildthecloud.dstack.configitem.version.ConfigItemStatusManager;
import io.github.ibuildthecloud.dstack.configitem.version.dao.ConfigItemStatusDao;
import io.github.ibuildthecloud.dstack.core.model.Agent;
import io.github.ibuildthecloud.dstack.core.model.ConfigItemStatus;
import io.github.ibuildthecloud.dstack.deferred.util.DeferredUtils;
import io.github.ibuildthecloud.dstack.eventing.model.Event;
import io.github.ibuildthecloud.dstack.iaas.config.ScopedConfig;
import io.github.ibuildthecloud.dstack.object.ObjectManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class ConfigItemStatusManagerImpl implements ConfigItemStatusManager {

    private static final Logger log = LoggerFactory.getLogger(ConfigItemStatusManagerImpl.class);

    ConfigItemStatusDao configItemStatusDao;
    ObjectManager objectManager;
    AgentLocator agentLocator;
    ScopedConfig scopedConfig;

    protected Map<String,ConfigItemStatus> getStatus(ConfigUpdateRequest request) {
        Map<String,ConfigItemStatus> statuses = new HashMap<String, ConfigItemStatus>();

        for ( ConfigItemStatus status : configItemStatusDao.listItems(request) ) {
            statuses.put(status.getName(), status);
        }

        return statuses;
    }

    @Override
    public void updateConfig(ConfigUpdateRequest request) {
        Client client = new DefaultClient(Agent.class, request.getAgentId());
        Map<String,ConfigItemStatus> statuses = getStatus(request);
        List<ConfigUpdateItem> toTrigger = new ArrayList<ConfigUpdateItem>();

        for ( ConfigUpdateItem item : request.getItems() ) {
            boolean modified = false;
            String name = item.getName();
            ConfigItemStatus status = statuses.get(name);
            Long requestedVersion = item.getRequestedVersion();

            if ( status == null ) {
                if ( item.isApply() ) {
                    requestedVersion = configItemStatusDao.incrementOrApply(client, name);
                    modified = true;
                }
            }

            if ( requestedVersion == null && item.isIncrement() ) {
                requestedVersion = configItemStatusDao.incrementOrApply(client, name);
                modified = true;
            }

            if ( requestedVersion == null ) {
                requestedVersion = configItemStatusDao.getRequestedVersion(client, name);
            }

            item.setRequestedVersion(requestedVersion);

            if ( modified ) {
                toTrigger.add(item);
            }
        }

        triggerUpdate(request, toTrigger);
    }

    protected void triggerUpdate(ConfigUpdateRequest request, List<ConfigUpdateItem> items) {
        final Event event = getEvent(request, items);
        if ( event == null ) {
            return;
        }

        final RemoteAgent agent = agentLocator.lookupAgent(request.getAgentId());
        Runnable run = new Runnable() {
            @Override
            public void run() {
                agent.publish(event);
            }
        };

        if ( request.isDeferredTrigger() ) {
            DeferredUtils.defer(run);
        } else {
            run.run();
        }
    }

    protected Event getEvent(ConfigUpdateRequest request, List<ConfigUpdateItem> items) {
        if ( items.size() == 0 ) {
            return null;
        }

        String url = scopedConfig.getConfigUrl(Agent.class, request.getAgentId());
        ConfigUpdate event = new ConfigUpdate(url, items);

        event.withResourceType(objectManager.getType(Agent.class))
            .withResourceId(Long.toString(request.getAgentId()));

        return event;
    }

    @Override
    public ListenableFuture<?> whenReady(final ConfigUpdateRequest request) {
        List<ConfigUpdateItem> toTrigger = getNeedsUpdating(request);
        Event event = getEvent(request, toTrigger);

        if ( event == null ) {
            return AsyncUtils.done();
        }

        RemoteAgent agent = agentLocator.lookupAgent(request.getAgentId());

        return Futures.transform(agent.call(event), new Function<Object, Object>() {
            @Override
            public Object apply(Object input) {
                List<ConfigUpdateItem> toTrigger = getNeedsUpdating(request);
                if ( toTrigger.size() > 0 ) {
                    throw new ConfigTimeoutException(request, toTrigger);
                }

                return Boolean.TRUE;
            }
        });
    }

    protected List<ConfigUpdateItem> getNeedsUpdating(ConfigUpdateRequest request) {
        Client client = new DefaultClient(Agent.class, request.getAgentId());
        Map<String,ConfigItemStatus> statuses = getStatus(request);
        List<ConfigUpdateItem> toTrigger = new ArrayList<ConfigUpdateItem>();

        for ( ConfigUpdateItem item : request.getItems() ) {
            String name = item.getName();
            ConfigItemStatus status = statuses.get(item.getName());

            if ( status == null ) {
                log.error("Waiting on config item [{}] on client [{}] but it is not applied", name, client);
                continue;
            }

            if ( item.isCheckInSync() ) {
                if ( ! ObjectUtils.equals(status.getRequestedVersion(), status.getAppliedVersion()) ) {
                    log.info("Waiting on [{}] on [{}], not in sync requested [{}] != applied [{}]",
                            client, name, status.getRequestedVersion(), status.getAppliedVersion());
                    toTrigger.add(item);
                }
            } else if ( item.getRequestedVersion() != null ) {
                Long applied = status.getAppliedVersion();
                if ( applied == null || item.getRequestedVersion() < applied ) {
                    log.info("Waiting on [{}] on [{}], not applied requested [{}] < applied [{}]",
                            client, name, item.getRequestedVersion(), applied);
                    toTrigger.add(item);
                }
            }
        }

        return toTrigger;
    }

    @Override
    public void waitFor(ConfigUpdateRequest request) {
        AsyncUtils.get(whenReady(request));
    }

    @Override
    public boolean setApplied(Client client, String itemName, ItemVersion version) {
        return configItemStatusDao.setApplied(client, itemName, version);
    }

    @Override
    public void setLatest(Client client, String itemName, String sourceRevision) {
        configItemStatusDao.setLatest(client, itemName, sourceRevision);
    }

    @Override
    public boolean isAssigned(Client client, String itemName) {
        return configItemStatusDao.isAssigned(client, itemName);
    }

    @Override
    public void setItemSourceVersion(String name, String sourceRevision) {
        configItemStatusDao.setItemSourceVersion(name, sourceRevision);
    }

    @Override
    public ItemVersion getRequestedVersion(Client client, String itemName) {
        return configItemStatusDao.getRequestedItemVersion(client, itemName);
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    public ConfigItemStatusDao getConfigItemStatusDao() {
        return configItemStatusDao;
    }

    @Inject
    public void setConfigItemStatusDao(ConfigItemStatusDao configItemStatusDao) {
        this.configItemStatusDao = configItemStatusDao;
    }

    public AgentLocator getAgentLocator() {
        return agentLocator;
    }

    @Inject
    public void setAgentLocator(AgentLocator agentLocator) {
        this.agentLocator = agentLocator;
    }

    public ScopedConfig getScopedConfig() {
        return scopedConfig;
    }

    @Inject
    public void setScopedConfig(ScopedConfig scopedConfig) {
        this.scopedConfig = scopedConfig;
    }

}