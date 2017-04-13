package io.cattle.platform.agent.connection.simulator.impl;

import io.cattle.platform.agent.connection.simulator.AgentConnectionSimulator;
import io.cattle.platform.agent.connection.simulator.AgentSimulatorEventProcessor;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.framework.event.FrameworkEvents;
import io.cattle.platform.framework.event.Ping;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class SimulatorPingProcessor implements AgentSimulatorEventProcessor {

    JsonMapper jsonMapper;
    ObjectManager objectManager;

    @Override
    public Event handle(AgentConnectionSimulator simulator, Event event) throws Exception {
        if (!FrameworkEvents.PING.equals(event.getName()))
            return null;

        Agent agent = simulator.getAgent();
        Ping ping = jsonMapper.convertValue(event, Ping.class);
        Ping pong = jsonMapper.convertValue(EventVO.reply(event).withData(ping.getData()), Ping.class);

        if (ping.getOption(Ping.RESOURCES) && !agent.getUri().startsWith("delegate://")) {
            addResources(pong, agent);
        }

        if (ping.getOption(Ping.INSTANCES) && !agent.getUri().startsWith("delegate://")) {
            addInstances(simulator, pong, agent);
        }

        return pong;
    }

    protected void addInstances(AgentConnectionSimulator simulator, Ping pong, Agent agent) {
        List<Map<String, Object>> resources = pong.getData().getResources();

        for (Map.Entry<String, Object[]> kv : simulator.getInstances().entrySet()) {
            // This matches the data structure returned by the ping logic in the real ping agent.
            Map<String, Object> instanceMap = CollectionUtils.asMap(
                    ObjectMetaDataManager.TYPE_FIELD, InstanceConstants.TYPE,
                    ObjectMetaDataManager.UUID_FIELD, kv.getKey(),
                    ObjectMetaDataManager.STATE_FIELD, kv.getValue()[0],
                    "systemContainer", null,
                    "dockerId", kv.getValue()[1],
                    "image", kv.getValue()[2],
                    "labels", new String[0],
                    "created", kv.getValue()[3]);
            resources.add(instanceMap);
        }

        String hostUuid = agent.getUuid() + "-" + 0;
        Map<String, Object> hostUuidResource = new HashMap<String, Object>();
        hostUuidResource.put(ObjectMetaDataManager.TYPE_FIELD, "host");
        hostUuidResource.put(ObjectMetaDataManager.UUID_FIELD, hostUuid);
        resources.add(hostUuidResource);

        pong.setOption(Ping.INSTANCES, true);
    }

    protected void addResources(Ping pong, Agent agent) {
        List<Map<String, Object>> resources = pong.getData().getResources();

        String externalId = DataAccessor.fromDataFieldOf(agent).withScope(AgentConnectionSimulator.class).withKey("externalId").as(jsonMapper,
                String.class);
        String physicalHostUuid = externalId;

        if (StringUtils.isEmpty(physicalHostUuid)) {
            physicalHostUuid = agent.getUuid() + "-physical-host";
        }

        Map<String, Object> physicalHost = new HashMap<String, Object>();
        physicalHost.put(ObjectMetaDataManager.UUID_FIELD, physicalHostUuid);
        physicalHost.put(ObjectMetaDataManager.KIND_FIELD, "sim");
        physicalHost.put(ObjectMetaDataManager.TYPE_FIELD, "physicalHost");

        Boolean addPhysicalHost = DataAccessor.fromDataFieldOf(agent).withScope(AgentConnectionSimulator.class).withKey("addPhysicalHost").withDefault(true)
                .as(jsonMapper, Boolean.class);

        long hosts = DataAccessor.fromDataFieldOf(agent).withScope(AgentConnectionSimulator.class).withKey("hosts").withDefault(1L).as(jsonMapper, Long.class);

        long poolsPerHost = DataAccessor.fromDataFieldOf(agent).withScope(AgentConnectionSimulator.class).withKey("storagePoolsPerHost").withDefault(1L).as(
                jsonMapper, Long.class);

        for (long i = 0; i < hosts; i++) {
            String hostUuid = agent.getUuid() + "-" + i;
            if (i == 0 && StringUtils.isNotBlank(externalId)) {
                hostUuid = externalId;
            }

            Map<String, Object> host = new HashMap<String, Object>();
            host.put(ObjectMetaDataManager.UUID_FIELD, hostUuid);
            host.put(ObjectMetaDataManager.KIND_FIELD, "sim");
            host.put(ObjectMetaDataManager.TYPE_FIELD, "host");
            host.put(ObjectMetaDataManager.NAME_FIELD, "testhost-" + io.cattle.platform.util.resource.UUID.randomUUID());

            Long cpu = DataAccessor.fromDataFieldOf(agent).withScope(AgentConnectionSimulator.class)
                    .withKey(HostConstants.FIELD_MILLI_CPU).as(jsonMapper, Long.class);
            if (cpu != null) {
                host.put(HostConstants.FIELD_MILLI_CPU, cpu);
            }

            Long mem = DataAccessor.fromDataFieldOf(agent).withScope(AgentConnectionSimulator.class)
                    .withKey(HostConstants.FIELD_MEMORY).as(jsonMapper, Long.class);
            if (mem != null) {
                host.put(HostConstants.FIELD_MEMORY, mem);
            }

            Long storage = DataAccessor.fromDataFieldOf(agent).withScope(AgentConnectionSimulator.class)
                    .withKey(HostConstants.FIELD_LOCAL_STORAGE_MB).as(jsonMapper, Long.class);
            if (storage != null) {
                host.put(HostConstants.FIELD_LOCAL_STORAGE_MB, storage);
            }

            if (addPhysicalHost) {
                host.put("physicalHostUuid", physicalHostUuid);
            }

            for (long j = 0; j < poolsPerHost; j++) {
                String poolUuid = hostUuid + "-" + j;

                Map<String, Object> pool = new HashMap<String, Object>();
                pool.put(ObjectMetaDataManager.UUID_FIELD, poolUuid);
                pool.put(ObjectMetaDataManager.KIND_FIELD, "sim");
                pool.put(ObjectMetaDataManager.TYPE_FIELD, "storagePool");
                pool.put("hostUuid", hostUuid);

                resources.add(pool);
            }

            /*
             * Purposely put host after storagePool so that AgentResourceManager
             * will have to reorder then on insert
             */
            resources.add(host);

            String ipAddress = DataAccessor.fromDataFieldOf(agent)
                    .withScope(AgentConnectionSimulator.class)
                    .withKey("ipAddress")
                    .withDefault("192.168.0.21").as(String.class);
            String ipUuid = agent.getUuid() + "-" + ipAddress;

            Map<String, Object> ip = new HashMap<>();
            ip.put(ObjectMetaDataManager.UUID_FIELD, ipUuid);
            ip.put(ObjectMetaDataManager.KIND_FIELD, "sim");
            ip.put(ObjectMetaDataManager.TYPE_FIELD, "ipAddress");
            ip.put("address", ipAddress);
            ip.put("hostUuid", hostUuid);

            resources.add(ip);
        }


        if (addPhysicalHost) {
            /*
             * Purposely put physical host after host so that
             * AgentResourceManager will have to reorder then on insert
             */
            resources.add(physicalHost);
        }
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

}
