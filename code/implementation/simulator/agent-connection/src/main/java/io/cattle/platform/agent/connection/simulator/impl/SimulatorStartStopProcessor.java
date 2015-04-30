package io.cattle.platform.agent.connection.simulator.impl;

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
        Boolean add = null;

        if ("compute.instance.activate".equals(event.getName())) {
            add = true;
        } else if ("compute.instance.deactivate".equals(event.getName())) {
            add = false;
        }

        if (add == null) {
            return null;
        }

        String eventString = jsonMapper.writeValueAsString(event);

        final Object uuid = CollectionUtils.getNestedValue(event.getData(), "instanceHostMap", "instance", "uuid");
        if (uuid != null) {
            if (add && !FORGET.matcher(eventString).matches()) {
                simulator.getInstances().add(uuid.toString());
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

        Map<String, Object> instance = (Map<String, Object>)CollectionUtils.getNestedValue(event.getData(), "instanceHostMap", "instance");
        Map<String, Object> update = null;
        if (instance.get("externalId") == null) {
            update =
                    CollectionUtils.asMap("instanceHostMap",
                            CollectionUtils.asMap("instance", CollectionUtils.asMap("externalId", UUID.randomUUID().toString())));
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
