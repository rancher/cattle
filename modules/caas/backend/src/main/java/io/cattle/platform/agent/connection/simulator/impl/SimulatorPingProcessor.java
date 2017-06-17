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

public class SimulatorPingProcessor implements AgentSimulatorEventProcessor {

    JsonMapper jsonMapper;
    ObjectManager objectManager;

    public SimulatorPingProcessor(JsonMapper jsonMapper, ObjectManager objectManager) {
        this.jsonMapper = jsonMapper;
        this.objectManager = objectManager;
    }

    @Override
    public Event handle(AgentConnectionSimulator simulator, Event event) throws Exception {
        if (!FrameworkEvents.PING.equals(event.getName()))
            return null;

        Agent agent = simulator.getAgent();
        Ping ping = jsonMapper.convertValue(event, Ping.class);
        Ping pong = jsonMapper.convertValue(EventVO.reply(event).withData(ping.getData()), Ping.class);

        if (ping.getOption(Ping.RESOURCES)) {
            addResources(pong, agent);
        }

        if (ping.getOption(Ping.INSTANCES)) {
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

        pong.setOption(Ping.INSTANCES, true);
    }

    protected void addResources(Ping pong, Agent agent) {
        List<Map<String, Object>> resources = pong.getData().getResources();

        String externalId = DataAccessor.fromDataFieldOf(agent).withScope(AgentConnectionSimulator.class).withKey("externalId").as(
                String.class);

        Map<String, Object> host = new HashMap<>();
        host.put(ObjectMetaDataManager.KIND_FIELD, "sim");
        host.put(ObjectMetaDataManager.TYPE_FIELD, "host");
        host.put(ObjectMetaDataManager.NAME_FIELD, "testhost-" + io.cattle.platform.util.resource.UUID.randomUUID());

        Long cpu = DataAccessor.fromDataFieldOf(agent).withScope(AgentConnectionSimulator.class)
                .withKey(HostConstants.FIELD_MILLI_CPU).as(Long.class);
        if (cpu != null) {
            host.put(HostConstants.FIELD_MILLI_CPU, cpu);
        }

        Long mem = DataAccessor.fromDataFieldOf(agent).withScope(AgentConnectionSimulator.class)
                .withKey(HostConstants.FIELD_MEMORY).as(Long.class);
        if (mem != null) {
            host.put(HostConstants.FIELD_MEMORY, mem);
        }

        Long storage = DataAccessor.fromDataFieldOf(agent).withScope(AgentConnectionSimulator.class)
                .withKey(HostConstants.FIELD_LOCAL_STORAGE_MB).as(Long.class);
        if (storage != null) {
            host.put(HostConstants.FIELD_LOCAL_STORAGE_MB, storage);
        }

        Map<String, Object> pool = new HashMap<>();
        pool.put(ObjectMetaDataManager.KIND_FIELD, "sim");
        pool.put(ObjectMetaDataManager.TYPE_FIELD, "storagePool");

        resources.add(pool);

        /*
         * Purposely put host after storagePool so that AgentResourceManager
         * will have to reorder then on insert
         */
        resources.add(host);

        String ipAddress = DataAccessor.fromDataFieldOf(agent)
                .withScope(AgentConnectionSimulator.class)
                .withKey("ipAddress")
                .withDefault("192.168.0.21").as(String.class);
        String ipUuid = agent.getUuid().substring(0, 8) + "-" + ipAddress;

        Map<String, Object> ip = new HashMap<>();
        ip.put(ObjectMetaDataManager.KIND_FIELD, "sim");
        ip.put(ObjectMetaDataManager.UUID_FIELD, ipUuid);
        ip.put(ObjectMetaDataManager.TYPE_FIELD, "ipAddress");
        ip.put("address", ipAddress);

        resources.add(ip);
    }

}
