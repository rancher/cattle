package io.github.ibuildthecloud.agent.connection.simulator.impl;

import io.github.ibuildthecloud.agent.connection.simulator.AgentConnectionSimulator;
import io.github.ibuildthecloud.agent.connection.simulator.AgentSimulatorEventProcessor;
import io.github.ibuildthecloud.dstack.core.model.Agent;
import io.github.ibuildthecloud.dstack.eventing.model.Event;
import io.github.ibuildthecloud.dstack.eventing.model.EventVO;
import io.github.ibuildthecloud.dstack.framework.event.FrameworkEvents;
import io.github.ibuildthecloud.dstack.framework.event.Ping;
import io.github.ibuildthecloud.dstack.json.JsonMapper;
import io.github.ibuildthecloud.dstack.object.ObjectManager;
import io.github.ibuildthecloud.dstack.object.meta.ObjectMetaDataManager;
import io.github.ibuildthecloud.dstack.object.util.DataAccessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class SimulatorPingProcessor implements AgentSimulatorEventProcessor {

    JsonMapper jsonMapper;
    ObjectManager objectManager;

    @Override
    public Event handle(AgentConnectionSimulator simulator, Event event) throws Exception {
        if ( ! FrameworkEvents.PING.equals(event.getName()) )
            return null;

        Agent agent = simulator.getAgent();
        Ping ping = jsonMapper.convertValue(event, Ping.class);
        Ping pong = jsonMapper.convertValue(EventVO.reply(event).withData(ping.getData()),
                Ping.class);

        if ( ping.getOption(Ping.RESOURCES) ) {
            addResource(pong, agent);
        }

        return pong;
    }

    protected void addResource(Ping pong, Agent agent) {
        List<Map<String,Object>> resources = pong.getData().getResources();

        long hosts = DataAccessor
            .fromDataFieldOf(agent)
            .withScope(AgentConnectionSimulator.class)
            .withKey("hosts")
            .withDefault(1L)
            .as(jsonMapper, Long.class);

        long poolsPerHost = DataAccessor
                .fromDataFieldOf(agent)
                .withScope(AgentConnectionSimulator.class)
                .withKey("storagePoolsPerHost")
                .withDefault(1L)
                .as(jsonMapper, Long.class);

        for ( long i = 0 ; i < hosts ; i++ ) {
            String hostUuid = agent.getUuid() + "-" + i;

            Map<String,Object> host = new HashMap<String, Object>();
            host.put(ObjectMetaDataManager.UUID_FIELD, hostUuid);
            host.put(ObjectMetaDataManager.KIND_FIELD, "sim");
            host.put(ObjectMetaDataManager.TYPE_FIELD, "host");

            for ( long j = 0 ; j < poolsPerHost ; j++ ) {
                String poolUuid = hostUuid + "-" + j;

                Map<String,Object> pool = new HashMap<String, Object>();
                pool.put(ObjectMetaDataManager.UUID_FIELD, poolUuid);
                pool.put(ObjectMetaDataManager.KIND_FIELD, "sim");
                pool.put(ObjectMetaDataManager.TYPE_FIELD, "storagePool");
                pool.put("hostUuid", hostUuid);

                resources.add(pool);
            }

            /* Purposely put host after storagePool so that AgentResourceManager
             * will have to reorder then on insert
             */
            resources.add(host);
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
