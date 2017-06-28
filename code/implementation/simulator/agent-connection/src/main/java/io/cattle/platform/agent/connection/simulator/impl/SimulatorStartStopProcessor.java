package io.cattle.platform.agent.connection.simulator.impl;

import static io.cattle.platform.core.constants.InstanceConstants.*;

import io.cattle.platform.agent.connection.simulator.AgentConnectionSimulator;
import io.cattle.platform.agent.connection.simulator.AgentSimulatorEventProcessor;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class SimulatorStartStopProcessor implements AgentSimulatorEventProcessor {

    private static final Pattern SHUTDOWN = Pattern.compile(".*simShutdownAfter\",\"([0-9]+)");
    private static final Pattern FORGET = Pattern.compile(".*simForgetImmediately.*");
    private static final Pattern DISCONNECT = Pattern.compile(".*simDisconnectAgent.*");
    private static final String SIM_CREATE_ANOTHER = "simCreateAnother_";
    private static final Pattern CREATE_ANOTHER = Pattern.compile(".*simCreateAnother_.*");

    ObjectManager objectManager;
    JsonMapper jsonMapper;
    ScheduledExecutorService scheduleExecutorService;

    public SimulatorStartStopProcessor(ObjectManager objectManager, JsonMapper jsonMapper, ScheduledExecutorService scheduleExecutorService) {
        super();
        this.objectManager = objectManager;
        this.jsonMapper = jsonMapper;
        this.scheduleExecutorService = scheduleExecutorService;
    }

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
            externalId = io.cattle.platform.util.resource.UUID.randomUUID().toString();
            update = CollectionUtils.asMap("instanceHostMap", CollectionUtils.asMap("instance", CollectionUtils.asMap("externalId", externalId)));
        }

        final Object uuid = instance.get("uuid");
        Object image = CollectionUtils.getNestedValue(instance, "data", "fields", "imageUuid");
        String imageUuid = image != null ? image.toString() : "sim:foo";
        if (uuid != null) {
            boolean found = simulator.getInstances().containsKey(uuid.toString());
            boolean forget = FORGET.matcher(eventString).matches();
            if (forget) {
                simulator.getInstances().remove(uuid.toString());
            } else if ("add".equals(action)) {
                simulator.getInstances().put(uuid.toString(), new Object[] { STATE_RUNNING, externalId, imageUuid, new Date().getTime() });
            } else if ("stop".equals(action) && found) {
                simulator.getInstances().put(uuid.toString(), new Object[] { STATE_STOPPED, externalId, imageUuid, new Date().getTime() });
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

        if (DISCONNECT.matcher(eventString).matches()) {
            simulator.setOpen(false);
        }

        if (CREATE_ANOTHER.matcher(eventString).matches()) {
            String name = (String)instance.get("name");
            String anotherExternalId = StringUtils.substringAfter(name, SIM_CREATE_ANOTHER);
            simulator.getInstances().put("name-" + anotherExternalId, new Object[] { STATE_RUNNING, anotherExternalId, imageUuid,
                    new Date().getTime() });
        }

        return EventVO.reply(event).withData(update);
    }

}
