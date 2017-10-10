package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.PortTable.*;

import io.cattle.platform.allocator.service.AllocatorService;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.iaas.event.IaasEvents;
import io.cattle.platform.json.JsonMapper;
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
import javax.inject.Named;

@Named
public class LoadBalancerServiceUpdatePostListener extends AbstractObjectProcessLogic implements ProcessPostListener,
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
    AllocatorService allocatorService;
    @Inject
    EventService eventService;

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

        Map<String, Object> launchConfigData = ServiceDiscoveryUtil.getLaunchConfigDataAsMap(service,
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

            allocatorService.allocatePortsForInstanceUpdate(instance, toCreate);
            
            for (Port port : toRetain.values()) {
                createThenActivate(port, new HashMap<String, Object>());
            }

            for (Port port : toRemove) {
                deactivateThenRemove(port, new HashMap<String, Object>());
            }
            
            allocatorService.releasePortsForInstanceUpdate(instance, toRemove);
            updateInstanceWithNewPorts(instance);
        }
    }
    
    private void updateInstanceWithNewPorts(Instance instance) {
        List<Port> toUpdate = objectManager.find(Port.class, PORT.INSTANCE_ID, instance.getId(), PORT.REMOVED, null);
        List<String> toUpdatePortDefs = new ArrayList<>();

        for (Port port : toUpdate) {
            PortSpec portSpec = new PortSpec(port);
            toUpdatePortDefs.add(portSpec.toSpec());
        }

        instance = objectManager.setFields(instance, InstanceConstants.FIELD_PORTS, toUpdatePortDefs);
        // trigger instance/metadata update
        Event event = EventVO.newEvent(IaasEvents.INVALIDATE_INSTANCE_DATA_CACHE)
                .withResourceType(instance.getKind())
                .withResourceId(instance.getId().toString());
        eventService.publish(event);
        return;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

    protected void removePort(Port port) {
        deactivateThenRemove(port, null);
    }
}
