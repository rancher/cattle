package io.cattle.platform.agent.connection.simulator.impl;

import io.cattle.platform.agent.connection.simulator.AgentConnectionSimulator;
import io.cattle.platform.agent.connection.simulator.AgentSimulatorEventProcessor;
import io.cattle.platform.core.addon.DeploymentSyncResponse;
import io.cattle.platform.core.addon.InstanceStatus;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.util.type.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.cattle.platform.core.constants.InstanceConstants.*;

public class SimulatorStartStopProcessor implements AgentSimulatorEventProcessor {

    private static final Pattern SHUTDOWN = Pattern.compile(".*simShutdownAfter\",\"([0-9]+)");
    private static final Pattern FORGET = Pattern.compile(".*simForgetImmediately.*");
    private static final Pattern DISCONNECT = Pattern.compile(".*simDisconnectAgent.*");
    private static final String SIM_CREATE_ANOTHER = "simCreateAnother_";
    private static final Pattern CREATE_ANOTHER = Pattern.compile(".*simCreateAnother_.*");
    private static final Set<String> EVENTS = CollectionUtils.set(
            "compute.instance.activate",
            "compute.instance.deactivate",
            "compute.instance.remove"
    );

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

        if (!EVENTS.contains(event.getName())) {
            return null;
        }

        List<?> instances = CollectionUtils.toList(CollectionUtils.getNestedValue(event.getData(), "deploymentSyncRequest", "containers"));
        if (instances.size() == 0) {
            return null;
        }

        String eventString = jsonMapper.writeValueAsString(event);
        DeploymentSyncResponse response = new DeploymentSyncResponse();

        Map<String, Object> instance = CollectionUtils.toMap(instances.get(0));
        String state = (String)instance.get("state");
        String uuid = (String)instance.get("uuid");
        String externalId = (String)instance.get("externalId");
        if (externalId == null) {
            externalId = io.cattle.platform.util.resource.UUID.randomUUID().toString();
        }

        response.getInstanceStatus().add(new InstanceStatus(uuid, externalId));

        Object image = CollectionUtils.getNestedValue(instance, "data", "fields", "imageUuid");
        String imageUuid = image != null ? image.toString() : "sim:foo";
        if (uuid != null) {
            boolean found = simulator.getInstances().containsKey(uuid.toString());
            boolean forget = FORGET.matcher(eventString).matches();
            if (forget) {
                simulator.getInstances().remove(uuid.toString());
            } else if ("starting".equals(state)) {
                simulator.getInstances().put(uuid.toString(), new Object[] { STATE_RUNNING, externalId, imageUuid, new Date().getTime() });
            } else if ("stopping".equals(state) && found) {
                simulator.getInstances().put(uuid.toString(), new Object[] { STATE_STOPPED, externalId, imageUuid, new Date().getTime() });
            } else {
                simulator.getInstances().remove(uuid.toString());
            }
        }

        Matcher m = SHUTDOWN.matcher(eventString);
        if (m.matches()) {
            scheduleExecutorService.schedule((Runnable) () -> simulator.getInstances().remove(uuid.toString()), Long.parseLong(m.group(1)), TimeUnit.SECONDS);
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

        return EventVO.reply(event).withData(response);
    }

}
