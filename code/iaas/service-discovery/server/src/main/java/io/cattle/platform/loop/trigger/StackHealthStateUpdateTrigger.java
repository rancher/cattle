package io.cattle.platform.loop.trigger;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.engine.manager.LoopManager;
import io.cattle.platform.engine.model.Trigger;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.loop.LoopFactoryImpl;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StackHealthStateUpdateTrigger implements Trigger {

    private static final Set<String> instanceProcesses = CollectionUtils.set(
                InstanceConstants.PROCESS_STOP,
                InstanceConstants.PROCESS_REMOVE,
                InstanceConstants.PROCESS_START);

    InstanceDao instanceDao;
    ObjectManager objectManager;
    LoopManager loopManager;

    public StackHealthStateUpdateTrigger(InstanceDao instanceDao, ObjectManager objectManager, LoopManager loopManager) {
        super();
        this.instanceDao = instanceDao;
        this.objectManager = objectManager;
        this.loopManager = loopManager;
    }

    @Override
    public void trigger(ProcessInstance process) {
        Object resource = process.getResource();
        List<Service> services = new ArrayList<>();
        Set<Long> stackIds = new HashSet<>();

        if (resource instanceof Stack) {
            stackIds.add(((Stack) resource).getId());
        } else if (resource instanceof Service) {
            services.add((Service) resource);
        } else if (resource instanceof Instance && instanceProcesses.contains(process.getName())) {
            services.addAll(instanceDao.findServicesFor((Instance) resource));
        } else if (resource instanceof DeploymentUnit) {
            Service service = objectManager.loadResource(Service.class,
                    ((DeploymentUnit) resource).getServiceId());
            if (service != null) {
                services.add(service);
            }
        }

        for (Service service : services) {
            stackIds.add(service.getStackId());
        }

        for (Long stackId : stackIds) {
            if (stackId == null) {
                continue;
            }

            loopManager.kick(LoopFactoryImpl.STACK_HEALTH, ServiceConstants.TYPE_STACK, stackId, null);
        }
    }

}

