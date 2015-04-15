package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.InstanceTable.INSTANCE;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.SERVICE_EXPOSE_MAP;
import static io.cattle.platform.core.model.tables.ServiceTable.SERVICE;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Image;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.process.progress.ProcessProgress;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.resource.ServiceDiscoveryConfigItem;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.storage.service.StorageService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ServiceActivate extends AbstractObjectProcessHandler {
    @Inject
    JsonMapper jsonMapper;

    @Inject
    StorageService storageService;

    @Inject
    ObjectManager objectManager;

    @Inject
    GenericMapDao mapDao;
    
    @Inject
    ResourceMonitor resourceMonitor;

    @Inject
    GenericResourceDao resourceDao;

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
        // on scale down, skip
        // the reason why I put >, not >= is - is for this handler to cover the case when instance was removed outside
        // of the service
        // and number of instances no longer reflects the service's scale
        // Example: 1) initial scale=3, instances compose-1 (Running), compose-2(Removed), compose-3 (Running)
        // 2) user wants to scaleDown to scale=2. To preserve the numbering in names, we will have to re-create
        // compose-2, and then remove compose-3 instance
        // 3) this hander will recreate compose-2 instance, and then antother posthandler will cover the scale down
        if (svcExposeDao.listNonRemovedInstancesForService(service.getId()).size() > scale) {
            return null;
        }

        List<Integer> consumedServiceIds = new ArrayList<>();
        boolean activateConsumedServices = DataAccessor.fromMap(state.getData()).withScope(ServiceActivate.class)
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

        createServiceInstances(service, scale);
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

    private void createServiceInstances(Service service, int scale) {
        List<Instance> instancesToStart = new ArrayList<>();
        Map<String, Object> launchConfigData = sdServer.buildLaunchData(service);
        Long imageId = getImage(String.valueOf(launchConfigData.get(InstanceConstants.FIELD_IMAGE_UUID)));
        List<Long> networkIds = getServiceNetworks(service);
        for (int i = 0; i < scale; i++) {
            Instance instance = createInstance(service, i, launchConfigData, imageId, networkIds);
            createInstanceServiceMap(instance, service);
            instancesToStart.add(instance);
        }
        startServiceInstances(instancesToStart);
    }

    private void startServiceInstances(List<Instance> instancesToStart) {
        for (Instance instance : instancesToStart) {
            scheduleStart(instance);
        }
        for (Instance instance : instancesToStart) {
            progress.checkPoint("start service instance " + instance.getUuid());
            instance = resourceMonitor.waitFor(instance, new ResourcePredicate<Instance>() {
                @Override
                public boolean evaluate(Instance obj) {
                    return InstanceConstants.STATE_RUNNING.equals(obj.getState());
                }
            });
        }
    }

    protected void scheduleStart(final Instance instance) {
        if (InstanceConstants.STATE_STOPPED.equals(instance.getState())) {
            DeferredUtils.nest(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    objectProcessManager.scheduleProcessInstance(InstanceConstants.PROCESS_START, instance, null);
                    return null;
                }
            });
        }
    }


    private List<Long> getServiceNetworks(Service service) {
        List<Long> ntwkIds = new ArrayList<>();
        long networkId = sdServer.getServiceNetworkId(service);
        ntwkIds.add(networkId);
        return ntwkIds;
    }

    protected Long getImage(String imageUuid) {
        Image image;
        try {
            image = storageService.registerRemoteImage(imageUuid);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to get image [" + imageUuid + "]");
        }

        return image == null ? null : image.getId();
    }


    protected Instance createInstance(Service service, int i, Map<String, Object> launchConfigData, Long imageId,
            List<Long> networkIds) {
        String instanceName = sdServer.getInstanceName(service, i);
        Instance instance = objectManager.findOne(Instance.class, INSTANCE.NAME, instanceName,
                INSTANCE.REMOVED, null, INSTANCE.ACCOUNT_ID, service.getAccountId());

        if (instance == null) {
            Map<Object, Object> properties = new HashMap<Object, Object>();
            properties.putAll(launchConfigData);
            properties.put(INSTANCE.NAME, instanceName);
            properties.put(INSTANCE.ACCOUNT_ID, service.getAccountId());
            properties.put(INSTANCE.KIND, InstanceConstants.KIND_CONTAINER);
            properties.put(InstanceConstants.FIELD_NETWORK_IDS, networkIds);
            properties.put(INSTANCE.IMAGE_ID, imageId);
            Map<String, Object> props = objectManager.convertToPropertiesFor(Instance.class,
                    properties);
            instance = resourceDao.createAndSchedule(Instance.class, props);
        }

        return instance;
    }

    protected void createInstanceServiceMap(Instance instance, Service service) {
        ServiceExposeMap instanceServiceMap = mapDao.findNonRemoved(ServiceExposeMap.class, Instance.class,
                instance.getId(),
                Service.class, service.getId());

        if (instanceServiceMap == null) {
            instanceServiceMap = objectManager.create(ServiceExposeMap.class, SERVICE_EXPOSE_MAP.INSTANCE_ID,
                    instance.getId(), SERVICE_EXPOSE_MAP.SERVICE_ID, service.getId());
        }
        objectProcessManager.executeStandardProcess(StandardProcess.CREATE, instanceServiceMap, null);
    }
}
