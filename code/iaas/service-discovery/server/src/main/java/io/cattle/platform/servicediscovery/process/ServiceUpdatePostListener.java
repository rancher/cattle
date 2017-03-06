package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.dao.ServiceExposeMapDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.util.type.Priority;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ServiceUpdatePostListener extends AbstractObjectProcessLogic implements ProcessPostListener,
        Priority {

    @Inject
    ServiceDiscoveryService sdService;
    @Inject
    JsonMapper jsonMapper;

    @Inject
    ServiceExposeMapDao expMapDao;

    @Inject
    NetworkDao ntwkDao;

    @Inject
    InstanceDao instanceDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceConstants.PROCESS_SERVICE_UPDATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Service service = (Service) state.getResource();
        if (!ServiceConstants.SERVICE_LIKE.contains(service.getKind())) {
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
        List<String> oldPortDefs = new ArrayList<>();
        if (oldObj != null) {
            Map<String, Object> old = (Map<String, Object>) oldObj;
            if (old.containsKey(ServiceConstants.FIELD_LAUNCH_CONFIG)) {
                Map<String, Object> oldLC = (Map<String, Object>) old.get(ServiceConstants.FIELD_LAUNCH_CONFIG);
                if (oldLC.get(InstanceConstants.FIELD_PORTS) != null) {
                    oldPortDefs = (List<String>) oldLC.get(InstanceConstants.FIELD_PORTS);
                }
            }
        }

        Map<String, Object> launchConfigData = ServiceUtil.getLaunchConfigDataAsMap(service,
                ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME);
        List<String> newPortDefs = new ArrayList<>();
        if (launchConfigData.get(InstanceConstants.FIELD_PORTS) != null) {
            newPortDefs = (List<String>) launchConfigData.get(InstanceConstants.FIELD_PORTS);
        }

        if (newPortDefs.containsAll(oldPortDefs) && oldPortDefs.containsAll(newPortDefs)) {
            return;
        }

        List<? extends Instance> serviceContainers = expMapDao.listServiceManagedInstancesAll(service);
        for (Instance instance : serviceContainers) {
            List<Port> toCreate = new ArrayList<>();
            List<Port> toRemove = new ArrayList<>();
            Map<String, Port> toRetain = new HashMap<>();
            // orchestrate port creation/removal
            ntwkDao.updateInstancePorts(instance, newPortDefs, toCreate, toRemove, toRetain);
            for (Port port : toCreate) {
                port = objectManager.create(port);
            }

            for (Port port : toRetain.values()) {
                createThenActivate(port, new HashMap<String, Object>());
            }

            for (Port port : toRemove) {
                deactivateThenRemove(port, new HashMap<String, Object>());
            }

            // trigger instance/metadata update
            instance = objectManager.setFields(instance, InstanceConstants.FIELD_PORTS, newPortDefs);
            instanceDao.clearCacheInstanceData(instance.getId());
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
