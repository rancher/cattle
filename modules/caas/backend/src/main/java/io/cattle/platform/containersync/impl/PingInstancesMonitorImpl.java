package io.cattle.platform.containersync.impl;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.containersync.PingInstancesMonitor;
import io.cattle.platform.containersync.ReportedInstance;
import io.cattle.platform.containersync.model.ContainerEventEvent;
import io.cattle.platform.core.addon.ContainerEvent;
import io.cattle.platform.core.addon.metadata.EnvironmentInfo;
import io.cattle.platform.core.addon.metadata.HostInfo;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.framework.event.FrameworkEvents;
import io.cattle.platform.framework.event.Ping;
import io.cattle.platform.framework.event.data.PingData;
import io.cattle.platform.metadata.Metadata;
import io.cattle.platform.metadata.MetadataManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.cattle.platform.core.constants.ContainerEventConstants.*;
import static io.cattle.platform.core.constants.HostConstants.*;
import static io.cattle.platform.core.constants.InstanceConstants.*;

public class PingInstancesMonitorImpl implements PingInstancesMonitor {

    ObjectManager objectManager;
    MetadataManager metadataManager;
    EventService eventService;
    AgentLocator agentLocator;

    public PingInstancesMonitorImpl(ObjectManager objectManager, MetadataManager metadataManager,
            EventService eventService, AgentLocator agentLocator) {
        this.objectManager = objectManager;
        this.metadataManager = metadataManager;
        this.eventService = eventService;
        this.agentLocator = agentLocator;
    }

    protected void addInstances(Map<String, String> knownExternalIdToState, Metadata metadata, HostInfo host) {
        metadata.getInstances().forEach(instance -> {
            if (instance.getHostId() != null && instance.getHostId() == host.getId()) {
                knownExternalIdToState.put(instance.getExternalId(), instance.getState());
            }
        });
    }

    @Override
    public void processPingReply(Agent agent, Ping ping) {
        List<Map<String, Object>> resources = getResources(ping);
        if (resources == null) {
            return;
        }

        String hostUuid = resources.stream()
                .filter(this::isHost)
                .map(this::getHostUuid)
                .findFirst().orElse(null);

        Metadata metadata = metadataManager.getMetadataForCluster(agent.getClusterId());
        HostInfo host = metadata.getHosts().stream()
                .filter((x) -> Objects.equals(hostUuid, x.getUuid()) || Objects.equals(agent.getId(), x.getAgentId()))
                .findFirst().orElse(null);
        if (host == null) {
            return;
        }

        Map<String, String> knownExternalIdToState = new HashMap<>();
        addInstances(knownExternalIdToState, metadata, host);
        for (EnvironmentInfo env : metadata.getEnvironments()) {
            addInstances(knownExternalIdToState, metadataManager.getMetadataForAccount(env.getAccountId()), host);
        }

        for (Map<String, Object> resource : resources) {
            if (!InstanceConstants.TYPE.equals(DataAccessor.fromMap(resource).withKey(ObjectMetaDataManager.TYPE_FIELD).get())) {
                continue;
            }

            ReportedInstance ri = new ReportedInstance(resource);
            String expectedState = knownExternalIdToState.remove(ri.getExternalId());
            if (expectedState == null) {
                importInstance(agent.getClusterId(), host.getId(), agent.getId(), ri);
            } else if (STATE_RUNNING.equals(expectedState) && STATE_STOPPED.equals(ri.getState())) {
                sendSimpleEvent(EVENT_STOP, host, ri.getExternalId());
            } else if (STATE_STOPPED.equals(expectedState) && STATE_RUNNING.equals(ri.getState())) {
                sendSimpleEvent(EVENT_START, host, ri.getExternalId());
            }
        }

        knownExternalIdToState.forEach((externalId, state) -> {
            if (STATE_RUNNING.equals(state) || STATE_STOPPED.equals(state)) {
                sendSimpleEvent(EVENT_DESTROY, host, externalId);
            }
        });
    }

    private void importInstance(long clusterId, long hostId, long agentId, ReportedInstance ri) {
        Event event = newInspectEvent(ri.getExternalId());
        RemoteAgent agent = agentLocator.lookupAgent(agentId);
        Futures.addCallback(agent.call(event), new FutureCallback<Event>() {
            @Override
            public void onSuccess(Event result) {
                Map<String, Object> inspect = CollectionUtils.toMap(CollectionUtils.getNestedValue(result.getData(), "instanceInspect"));
                ContainerEvent data = new ContainerEvent(clusterId, hostId, ri.getExternalId(), inspect);
                eventService.publish(new ContainerEventEvent(data));
            }

            @Override
            public void onFailure(Throwable t) {
            }
        });
    }

    private void sendSimpleEvent(String status, HostInfo host, String externalId) {
        ContainerEvent data = new ContainerEvent(status, host.getClusterId(), host.getId(), externalId);
        eventService.publish(new ContainerEventEvent(data));
    }

    protected List<Map<String, Object>> getResources(Ping ping) {
        PingData data = ping.getData();

        if (data == null) {
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
                .withResourceType("instanceInspect")
                .withData(CollectionUtils.asMap(
                    "instanceInspect", CollectionUtils.asMap(
                        "kind", "docker",
                        "id", externalId)
        ));
    }

}
