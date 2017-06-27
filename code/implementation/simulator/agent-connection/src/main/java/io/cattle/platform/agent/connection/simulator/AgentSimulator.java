package io.cattle.platform.agent.connection.simulator;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.annotation.AnnotatedEventListener;
import io.cattle.platform.eventing.annotation.EventHandler;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.eventing.util.EventUtils;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.resource.ResourceMonitor;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class AgentSimulator implements AnnotatedEventListener {

    private static final Simulator NULL_SIMULATOR = new NullSimulator();

    JsonMapper jsonMapper;
    ObjectManager objectManager;
    ResourceMonitor resourceMonitor;
    AgentLocator agentLocator;
    EventService eventService;

    List<AgentSimulatorEventProcessor> processors;
    LoadingCache<String, Simulator> cache = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build(new CacheLoader<String, Simulator>() {
        @Override
        public Simulator load(String agentId) throws Exception {
            return makeSimulator(agentId);
        }
    });

    public AgentSimulator(JsonMapper jsonMapper, ObjectManager objectManager, ResourceMonitor resourceMonitor, AgentLocator agentLocator,
            EventService eventService, AgentSimulatorEventProcessor... processors) {
        super();
        this.jsonMapper = jsonMapper;
        this.objectManager = objectManager;
        this.resourceMonitor = resourceMonitor;
        this.agentLocator = agentLocator;
        this.eventService = eventService;
        this.processors = Arrays.asList(processors);
    }

    @EventHandler
    public void agentRequest(SimulatorRequest agentRequest) {
        Simulator simulator = cache.getUnchecked(agentRequest.getResourceId());
        Event event = agentRequest.getData();
        Event resp = simulator.execute(event);
        if (resp == null) {
            return;
        }
        EventVO<?> reply = EventVO.reply(agentRequest).withData(resp);
        EventUtils.copyTransitioning(resp, reply);
        if (reply.getName() == null) {
            return;
        }
        eventService.publish(reply);
    }

    protected Simulator makeSimulator(String agentId) {
        Agent agent = objectManager.loadResource(Agent.class, agentId);
        if (agent == null) {
            return NULL_SIMULATOR;
        }

        String uri = agent.getUri();
        if (uri == null || !uri.startsWith("sim://")) {
            return NULL_SIMULATOR;
        }

        return new AgentConnectionSimulator(objectManager, agent, processors);
    }

}
