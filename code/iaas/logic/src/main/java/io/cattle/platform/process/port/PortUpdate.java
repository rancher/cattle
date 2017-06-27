package io.cattle.platform.process.port;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.iaas.event.IaasEvents;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.process.lock.InstancePortsLock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class PortUpdate extends AbstractDefaultProcessHandler {

    @Inject
    LockManager lockManager;
    @Inject
    EventService eventService;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Port port = (Port)state.getResource();
        final Instance instance = getObjectManager().loadResource(Instance.class, port.getInstanceId());

        if (instance == null) {
            return null;
        }

        lockManager.lock(new InstancePortsLock(instance), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                processPorts(instance);
            }
        });

        return null;
    }

    protected void processPorts(Instance instance) {
        Set<String> portSpecs = new HashSet<>();
        for (Port port : objectManager.children(instance, Port.class)) {
            if (port.getRemoved() != null) {
                continue;
            }

            portSpecs.add(new PortSpec(port).toSpec());
        }
        objectManager.setFields(instance, InstanceConstants.FIELD_PORTS, new ArrayList<>(portSpecs));
        Event event = EventVO.newEvent(IaasEvents.INVALIDATE_INSTANCE_DATA_CACHE)
                .withResourceType(instance.getKind())
                .withResourceId(instance.getId().toString());
        eventService.publish(event);
    }

}
