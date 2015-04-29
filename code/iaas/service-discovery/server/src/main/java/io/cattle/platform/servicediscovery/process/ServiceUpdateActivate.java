package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.ServiceTable.SERVICE;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.process.progress.ProcessProgress;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants.KIND;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.resource.ServiceDiscoveryConfigItem;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * This handler is responsible for activating the service as well as restoring the active service to its scale
 * The handler can be invoked as a part of service.activate, service.update for both scaleUp and ScaleDown
 *
 */
@Named
public class ServiceUpdateActivate extends AbstractObjectProcessHandler {
    @Inject
    JsonMapper jsonMapper;
    
    @Inject
    ResourceMonitor resourceMonitor;

    @Inject
    ProcessProgress progress;

    @Inject
    ServiceDiscoveryService sdServer;

    @Inject
    ServiceExposeMapDao svcExposeDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceDiscoveryConstants.PROCESS_SERVICE_ACTIVATE,
                ServiceDiscoveryConstants.PROCESS_SERVICE_UPDATE };
    }

    @Override
    @SuppressWarnings("unchecked")
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Service service = (Service) state.getResource();

        // on inactive service update, do nothing
        if (process.getName().equalsIgnoreCase(ServiceDiscoveryConstants.PROCESS_SERVICE_UPDATE)
                && service.getState().equalsIgnoreCase(CommonStatesConstants.UPDATING_INACTIVE)) {
            return null;
        }
        int scale = DataAccessor.field(service, ServiceDiscoveryConstants.FIELD_SCALE,
                jsonMapper,
                Integer.class);
        if (scale == 0) {
            return null;
        }

        List<Integer> consumedServiceIds = new ArrayList<>();
        boolean activateConsumedServices = DataAccessor.fromMap(state.getData()).withScope(ServiceUpdateActivate.class)
                .withKey(ServiceDiscoveryConstants.FIELD_ACTIVATE_CONSUMED_SERVICES).withDefault(false)
                .as(Boolean.class);
        if (activateConsumedServices) {
            // the order is determined only based on volumes_from (service) attribute
            // we don't look at service links because the services get linked via DNS updates,
            // and DNS update logic is going to be handled separately from service.activate
            List<? extends Integer> services = (List<? extends Integer>) DataUtils.getFields(service).get(
                    ServiceDiscoveryConfigItem.VOLUMESFROMSERVICE.getRancherName());
            if (services != null) {
                consumedServiceIds.addAll(services);
            }
        }

        progress.init(state, sdServer.getWeights(scale + consumedServiceIds.size(), 100));

        if (activateConsumedServices) {
            activateConsumedServices(consumedServiceIds, state.getData());
        }

        if (service.getKind().equalsIgnoreCase(KIND.SERVICE.name())) {
            sdServer.activateService(service, scale);
        } else if (service.getKind().equalsIgnoreCase(KIND.LOADBALANCERSERVICE.name())) {
            sdServer.activateLoadBalancerService(service, scale);
        }
        return null;
    }

    private void activateConsumedServices(List<? extends Integer> consumedServiceIds,
            Map<String, Object> data) {
        List<Service> consumedServices = new ArrayList<>();
        if (consumedServiceIds != null) {
            for (Integer consumedServiceId : consumedServiceIds) {
                // TODO: move the method to the Dao, so all the services get loaded in a bulk
                Service consumedService = objectManager.findOne(Service.class, SERVICE.ID, consumedServiceId,
                        SERVICE.REMOVED, null);
                if (consumedService != null) {
                    consumedServices.add(consumedService);
                }
            }
        }
        
        for (Service consumedService : consumedServices) {
            try {
                objectProcessManager.scheduleProcessInstance(ServiceDiscoveryConstants.PROCESS_SERVICE_ACTIVATE,
                        consumedService, data);
            } catch (ProcessCancelException e) {
                // do nothing
            }
        }
        waitForServiceActivate(consumedServices);
    }

    private void waitForServiceActivate(List<Service> servicesToActivate) {
        for (Service service : servicesToActivate) {
            progress.checkPoint("activate consumed service " + service.getName());
            service = resourceMonitor.waitFor(service, new ResourcePredicate<Service>() {
                @Override
                public boolean evaluate(Service obj) {
                    return CommonStatesConstants.ACTIVE.equals(obj.getState());
                }
            });
        }
    }
}
