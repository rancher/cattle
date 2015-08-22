package io.cattle.platform.agent.connection.simulator.impl;

import static io.cattle.platform.core.constants.InstanceConstants.*;
import io.cattle.platform.agent.connection.simulator.AgentConnectionSimulator;
import io.cattle.platform.agent.connection.simulator.AgentSimulatorEventProcessor;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

public class SimulatorStartStopProcessor implements AgentSimulatorEventProcessor {

    private static final Pattern SHUTDOWN = Pattern.compile(".*simShutdownAfter\",\"([0-9]+)");
    private static final Pattern FORGET = Pattern.compile(".*simForgetImmediately.*");

    JsonMapper jsonMapper;
    ScheduledExecutorService scheduleExecutorService;

    @SuppressWarnings("unchecked")
    @Override
    public Event handle(final AgentConnectionSimulator simulator, Event event) throws Exception {
        String action = null;

        if ("compute.instance.activate".equals(event.getName())) {
            action = "add";
        } else if ("compute.instance.deactivate".equals(event.getName())) {
            action = "stop";
        } else if ("compute.instance.remove".equals(event.getName())) {
            action = "remove";
        }

        if (action == null) {
            return null;
        }

        String eventString = jsonMapper.writeValueAsString(event);

        Map<String, Object> instance = (Map<String, Object>)CollectionUtils.getNestedValue(event.getData(), "instanceHostMap", "instance");
        Map<String, Object> update = null;
        String externalId = (String)instance.get("externalId");
        if (externalId == null) {
            externalId = UUID.randomUUID().toString();
            update = CollectionUtils.asMap("instanceHostMap", CollectionUtils.asMap("instance", CollectionUtils.asMap("externalId", externalId)));
        }

        final Object uuid = instance.get("uuid");
        if (uuid != null) {
            boolean found = simulator.getInstances().containsKey(uuid.toString());
            boolean forget = FORGET.matcher(eventString).matches();
            if (forget) {
                simulator.getInstances().remove(uuid.toString());
            } else if ("add".equals(action)) {
                simulator.getInstances().put(uuid.toString(), new String[] { STATE_RUNNING, externalId });
            } else if ("stop".equals(action) && found) {
                simulator.getInstances().put(uuid.toString(), new String[] { STATE_STOPPED, externalId });
            } else {
                simulator.getInstances().remove(uuid.toString());
            }
        }

        Matcher m = SHUTDOWN.matcher(eventString);
        if (m.matches()) {
            scheduleExecutorService.schedule(new Runnable() {
                @Override
                public void run() {
                    simulator.getInstances().remove(uuid.toString());
                }
            }, Long.parseLong(m.group(1)), TimeUnit.SECONDS);
        }

        return EventVO.reply(event).withData(update);
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public ScheduledExecutorService getScheduleExecutorService() {
        return scheduleExecutorService;
    }

    @Inject
    public void setScheduleExecutorService(ScheduledExecutorService scheduleExecutorService) {
        this.scheduleExecutorService = scheduleExecutorService;
    }

}
