package io.cattle.platform.containersync.impl;

import static io.cattle.platform.core.constants.ContainerEventConstants.*;
import static io.cattle.platform.core.constants.HostConstants.*;
import static io.cattle.platform.core.constants.InstanceConstants.*;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.containersync.PingInstancesMonitor;
import io.cattle.platform.containersync.ReportedInstance;
import io.cattle.platform.containersync.model.ContainerEventEvent;
import io.cattle.platform.core.addon.ContainerEvent;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.environment.EnvironmentResourceManager;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.framework.event.FrameworkEvents;
import io.cattle.platform.framework.event.Ping;
import io.cattle.platform.framework.event.data.PingData;
import io.cattle.platform.metadata.model.HostInfo;
import io.cattle.platform.metadata.service.Metadata;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

public class PingInstancesMonitorImpl implements PingInstancesMonitor {

    LoadingCache<Long, Long> accountCache = CacheBuilder.newBuilder()
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .build(new CacheLoader<Long, Long>() {
                @Override
                public Long load(Long key) throws Exception {
                    return getAccountId(key);
                }
            });

    ObjectManager objectManager;
    EnvironmentResourceManager envResourceManager;
    EventService eventService;
    AgentLocator agentLocator;

    public PingInstancesMonitorImpl(ObjectManager objectManager, EnvironmentResourceManager envResourceManager,
            EventService eventService, AgentLocator agentLocator) {
        this.objectManager = objectManager;
        this.envResourceManager = envResourceManager;
        this.eventService = eventService;
        this.agentLocator = agentLocator;
    }

    @Override
    public void processPingReply(Ping ping) {
        List<Map<String, Object>> resources = getResources(ping);
        if (resources == null) {
            return;
        }

        String hostUuid = resources.stream()
                .filter(this::isHost)
                .map(this::getHostUuid)
                .findFirst().orElse(null);
        if (hostUuid == null) {
            return;
        }

        long agentId = Long.parseLong(ping.getResourceId());
        Long accountId = accountCache.getUnchecked(agentId);
        if (accountId < 0L) {
            accountCache.invalidate(agentId);
            return;
        }

        Metadata metadata = envResourceManager.getMetadata(accountId);
        HostInfo host = metadata.getHosts().stream()
                .filter((x) -> hostUuid.equals(x.getUuid()))
                .findFirst().orElse(null);
        if (host == null) {
            return;
        }

        Map<String, String> knownExternalIdToState = metadata.getInstances().stream()
                .filter((i) -> i.getHostId() != null && i.getHostId().longValue() == host.getId())
                .collect(Collectors.toMap(
                        (i) -> i.getExternalId(),
                        (i) -> i.getState()));


        for (Map<String, Object> resource : resources) {
            if (!InstanceConstants.TYPE.equals(DataAccessor.fromMap(resource).withKey(ObjectMetaDataManager.TYPE_FIELD).get())) {
                continue;
            }

            ReportedInstance ri = new ReportedInstance(resource);
            String expectedState = knownExternalIdToState.remove(ri.getExternalId());
            if (expectedState == null) {
                importInstance(accountId, host.getId(), agentId, ri);
            } else if (STATE_RUNNING.equals(expectedState) && STATE_STOPPED.equals(ri.getState())) {
                sendSimpleEvent(EVENT_STOP, ri, accountId);
            } else if (STATE_STOPPED.equals(expectedState) && STATE_RUNNING.equals(ri.getState())) {
                sendSimpleEvent(EVENT_START, ri, accountId);
            }
        }

        knownExternalIdToState.forEach((externalId, state) -> {
            if (STATE_RUNNING.equals(state) || STATE_STOPPED.equals(state)) {
                sendSimpleEvent(EVENT_DESTROY, externalId, accountId);
            }
        });
    }

    private void importInstance(long accountId, long hostId, long agentId, ReportedInstance ri) {
        Event event = newInspectEvent(ri.getExternalId());
        RemoteAgent agent = agentLocator.lookupAgent(agentId);
        Futures.addCallback(agent.call(event), new FutureCallback<Event>() {
            @Override
            public void onSuccess(Event result) {
                Map<String, Object> inspect = CollectionUtils.toMap(CollectionUtils.getNestedValue(result.getData(), "instanceInspect"));
                ContainerEvent data = new ContainerEvent(accountId, hostId, ri.getUuid(), ri.getExternalId(), inspect);
                eventService.publish(new ContainerEventEvent(data));
            }

            @Override
            public void onFailure(Throwable t) {
            }
        });
    }

    private void sendSimpleEvent(String status, String externalId, long accountId) {
        ContainerEvent data = new ContainerEvent(status, accountId, null, externalId);
        eventService.publish(new ContainerEventEvent(data));
    }
    private void sendSimpleEvent(String status, ReportedInstance ri, long accountId) {
        ContainerEvent data = new ContainerEvent(status, accountId, ri.getUuid(), ri.getExternalId());
        eventService.publish(new ContainerEventEvent(data));
    }

    private Long getAccountId(Long agentId) {
        Agent agent = objectManager.loadResource(Agent.class, agentId);
        if (agent == null || agent.getResourceAccountId() == null) {
            return -1L;
        }
        return agent.getResourceAccountId();
    }

    protected List<Map<String, Object>> getResources(Ping ping) {
        PingData data = ping.getData();

        if (data == null || ping.getResourceId() == null) {
            return null;
        }

        List<Map<String, Object>> resources = data.getResources();
        if (resources == null || !ping.getOption(Ping.INSTANCES)) {
            return null;
        }

        return resources;
    }

    protected boolean isHost(Map<String, Object> resource) {
        Object type = DataAccessor.fromMap(resource)
                .withKey(ObjectMetaDataManager.TYPE_FIELD).get();
        return FIELD_HOST_UUID.equals(type);
    }

    protected String getHostUuid(Map<String, Object> resource) {
        return DataAccessor.fromMap(resource)
                .withKey(ObjectMetaDataManager.UUID_FIELD)
                .as(String.class);
    }

    private Event newInspectEvent(String externalId) {
        return EventVO.newEvent(FrameworkEvents.INSPECT)
                .withData(CollectionUtils.asMap(
                    "instanceInspect", CollectionUtils.asMap(
                        "kind", "docker",
                        "id", externalId)
        ));
    }

}
