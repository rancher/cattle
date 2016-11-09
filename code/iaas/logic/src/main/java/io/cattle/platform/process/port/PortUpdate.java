package io.cattle.platform.process.port;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
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
    InstanceDao instanceDao;

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
        instanceDao.clearCacheInstanceData(instance.getId());
    }

}
