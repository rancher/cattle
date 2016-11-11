package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.util.type.Priority;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class LoadBalancerServiceUpdatePostListener extends AbstractObjectProcessLogic implements ProcessPostListener,
        Priority {

    @Inject
    ServiceDiscoveryService sdService;
    @Inject
    JsonMapper jsonMapper;

    @Inject
    ServiceExposeMapDao expMapDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceConstants.PROCESS_SERVICE_UPDATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Service service = (Service) state.getResource();
        if (!service.getKind().equalsIgnoreCase(ServiceConstants.KIND_LOAD_BALANCER_SERVICE)) {
            return null;
        }

        updateServicePorts(service, state);

        return null;
    }

    @SuppressWarnings("unchecked")
    private void updateServicePorts(Service service, ProcessState state) {
        sdService.setPorts(service);

        // only perform update when ports got changed
        Object oldObj = state.getData().get("old");
        List<String> oldPortSpecs = new ArrayList<>();
        if (oldObj != null) {
            Map<String, Object> old = (Map<String, Object>) oldObj;
            if (old.containsKey(ServiceConstants.FIELD_LAUNCH_CONFIG)) {
                Map<String, Object> oldLC = (Map<String, Object>) old.get(ServiceConstants.FIELD_LAUNCH_CONFIG);
                if (oldLC.get(InstanceConstants.FIELD_PORTS) != null) {
                    oldPortSpecs = (List<String>) oldLC.get(InstanceConstants.FIELD_PORTS);
                }
            }
        }

        Map<String, Object> launchConfigData = ServiceDiscoveryUtil.getLaunchConfigDataAsMap(service,
                ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME);
        List<String> portSpecs = new ArrayList<>();
        if (launchConfigData.get(InstanceConstants.FIELD_PORTS) != null) {
            portSpecs = (List<String>) launchConfigData.get(InstanceConstants.FIELD_PORTS);
        }

        if (portSpecs.containsAll(oldPortSpecs) && oldPortSpecs.containsAll(portSpecs)) {
            return;
        }

        List<? extends Instance> serviceContainers = expMapDao.listServiceManagedInstancesAll(service);
        for (Instance instance : serviceContainers) {
            instance = objectManager.setFields(instance, InstanceConstants.FIELD_PORTS, portSpecs);
            objectProcessManager.scheduleStandardProcess(StandardProcess.UPDATE, instance,
                    new HashMap<String, Object>());
        }
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

    protected void removePort(Port port) {
        deactivateThenRemove(port, null);
    }
}
