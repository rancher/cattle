package io.cattle.platform.servicediscovery.service.impl;

import static io.cattle.platform.core.model.tables.EnvironmentTable.ENVIRONMENT;
import static io.cattle.platform.core.model.tables.HostTable.HOST;
import static io.cattle.platform.core.model.tables.ImageTable.IMAGE;
import static io.cattle.platform.core.model.tables.InstanceTable.INSTANCE;
import static io.cattle.platform.core.model.tables.LoadBalancerTable.LOAD_BALANCER;
import static io.cattle.platform.core.model.tables.ServiceTable.SERVICE;
import io.cattle.iaas.lb.service.LoadBalancerService;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Image;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerHostMap;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.process.progress.ProcessProgress;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.dao.ServiceConsumeMapDao;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.resource.ServiceDiscoveryConfigItem;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.storage.service.StorageService;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.TransformerUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class ServiceDiscoveryServiceImpl implements ServiceDiscoveryService {

    @Inject
    GenericMapDao mapDao;

    @Inject
    ServiceConsumeMapDao consumeMapDao;

    @Inject
    ObjectManager objectManager;

    @Inject
    NetworkDao ntwkDao;

    @Inject
    List<RancherConfigToComposeFormatter> formatters;

    @Inject
    ResourceMonitor resourceMonitor;

    @Inject
    GenericResourceDao resourceDao;

    @Inject
    ProcessProgress progress;

    @Inject
    ObjectProcessManager objectProcessManager;

    @Inject
    StorageService storageService;

    @Inject
    LoadBalancerService lbService;

    @Inject
    ServiceExposeMapDao exposeMapDao;

    @Override
    public SimpleEntry<String, String> buildConfig(List<? extends Service> services) {
        return new SimpleEntry<String, String>(buildDockerComposeConfig(services), buildRancherComposeConfig(services));
    }

    @Override
    public String buildDockerComposeConfig(List<? extends Service> services) {
        Map<String, Object> dockerComposeData = createComposeData(services, true);
        return convertToYml(dockerComposeData);
    }

    @Override
    public String buildRancherComposeConfig(List<? extends Service> services) {
        Map<String, Object> dockerComposeData = createComposeData(services, false);
        return convertToYml(dockerComposeData);
    }

    private String convertToYml(Map<String, Object> dockerComposeData) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        return yaml.dump(dockerComposeData);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> createComposeData(List<? extends Service> servicesToExport, boolean forDockerCompose) {
        Map<String, Object> data = new HashMap<String, Object>();
        Collection<Long> servicesToExportIds = CollectionUtils.collect(servicesToExport,
                TransformerUtils.invokerTransformer("getId"));
        for (Service service : servicesToExport) {
            Map<String, Object> rancherServiceData = getServiceDataAsMap(service);
            Map<String, Object> composeServiceData = new HashMap<>();
            for (String rancherService : rancherServiceData.keySet()) {
                ServiceDiscoveryConfigItem item = ServiceDiscoveryConfigItem.getServiceConfigItemByRancherName(rancherService);
                if (item != null && item.isDockerComposeProperty() == forDockerCompose) {
                    Object value = rancherServiceData.get(rancherService);
                    boolean export = false;
                    if (value instanceof List) {
                        if (!((List<?>) value).isEmpty()) {
                            export = true;
                        }
                    } else if (value instanceof Map) {
                        if (!((Map<?, ?>) value).isEmpty()) {
                            export = true;
                        }
                    } else if (value instanceof Boolean) {
                        if (((Boolean) value).booleanValue()) {
                            export = true;
                        }
                    } else if (value != null) {
                        export = true;
                    }
                    if (export) {
                        composeServiceData.put(item.getComposeName(), value);
                        // for every lookup, do transform
                        Object formattedValue = null;
                        for (RancherConfigToComposeFormatter formatter : formatters) {
                            formattedValue = formatter.format(item, value);
                            if (formattedValue != null) {
                                break;
                            }
                        }
                        if (formattedValue != null) {
                            composeServiceData.put(item.getComposeName().toLowerCase(), formattedValue);
                        } else {
                            composeServiceData.put(item.getComposeName().toLowerCase(), value);
                        }

                    }
                }
            }
            
            if (forDockerCompose) {
                populateLinksForService(service, servicesToExportIds, composeServiceData);
                populateVolumesForService(service, servicesToExportIds, composeServiceData);
            }
            
            data.put(service.getName(), composeServiceData);
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    private void populateVolumesForService(Service service, Collection<Long> servicesToExportIds,
            Map<String, Object> composeServiceData) {
        List<String> namesCombined = new ArrayList<>();
        List<String> servicesNames = new ArrayList<>();
        List<Service> translateToInstances = new ArrayList<>();
        List<? extends Integer> consumedServiceIds = (List<? extends Integer>) getServiceDataAsMap(service).get(
                ServiceDiscoveryConfigItem.VOLUMESFROMSERVICE.getRancherName());

        if (consumedServiceIds != null) {
            for (Integer consumedServiceId : consumedServiceIds) {
                Service consumedService = objectManager.findOne(Service.class, SERVICE.ID,
                        consumedServiceId);

                if (servicesToExportIds.contains(consumedServiceId.longValue())) {
                    servicesNames.add(consumedService.getName());
                } else {
                    translateToInstances.add(consumedService);
                }
            }
        }

        // 1. add services names
        namesCombined.addAll(servicesNames);

        // 2. translate instances ids to names
        List<? extends Integer> instanceIds = (List<? extends Integer>) getServiceDataAsMap(service).get(
                ServiceDiscoveryConfigItem.VOLUMESFROM.getRancherName());

        if (instanceIds != null) {
            for (Integer instanceId : instanceIds) {
                Instance instance = objectManager.findOne(Instance.class, INSTANCE.ID, instanceId, INSTANCE.REMOVED,
                        null);
                String instanceName = getInstanceName(instance);
                if (instanceName != null) {
                    namesCombined.add(instanceName);
                }
            }
        }

        // 4. now translate all the references services that are not being imported, to instance names
        for (Service volumesFromInstance : translateToInstances) {
            List<Instance> instances = objectManager.mappedChildren(volumesFromInstance, Instance.class);
            for (Instance instance : instances) {
                String instanceName = getInstanceName(instance);
                if (instanceName != null) {
                    namesCombined.add(instanceName);
                }
            }
        }
        if (!namesCombined.isEmpty()) {
            composeServiceData.put(ServiceDiscoveryConfigItem.VOLUMESFROM.getComposeName(), namesCombined);
        }

        populateExternalLinksForService(service, composeServiceData, translateToInstances);
    }

    private void populateLinksForService(Service service, Collection<Long> servicesToExportIds,
            Map<String, Object> composeServiceData) {
        List<String> serviceLinks = new ArrayList<>();
        List<Service> externalLinksServices = new ArrayList<>();
        List<? extends ServiceConsumeMap> consumedServiceMaps = consumeMapDao.findConsumedServices(service.getId());
        for (ServiceConsumeMap consumedServiceMap : consumedServiceMaps) {
            Service consumedService = objectManager.findOne(Service.class, SERVICE.ID, consumedServiceMap.getConsumedServiceId());
            
            if (servicesToExportIds.contains(consumedServiceMap.getConsumedServiceId())) {
                serviceLinks.add(consumedService.getName());
            } else {
                externalLinksServices.add(consumedService);
            }
        }
        if (!serviceLinks.isEmpty()) {
            composeServiceData.put(ServiceDiscoveryConfigItem.LINKS.getComposeName(), serviceLinks);
        }
        populateExternalLinksForService(service, composeServiceData, externalLinksServices);
    }

    @SuppressWarnings("unchecked")
    private void populateExternalLinksForService(Service service, Map<String, Object> composeServiceData,
            List<Service> externalLinksServices) {

        Map<String, String> instanceLinksWithNames = new LinkedHashMap<String, String>();
        Map<String, Object> instanceLinksWithIds = (Map<String, Object>) composeServiceData
                .get(ServiceDiscoveryConfigItem.EXTERNALLINKS
                .getComposeName());
        if (instanceLinksWithIds != null) {
            for (String linkName : instanceLinksWithIds.keySet()) {
                Instance instance = objectManager.findOne(Instance.class, INSTANCE.ID,
                        instanceLinksWithIds.get(linkName), INSTANCE.REMOVED,
                        null);
                String instanceName = getInstanceName(instance);
                if (instanceName != null) {
                    instanceLinksWithNames.put(linkName, instanceName);
                }
            }
        }

        if (!externalLinksServices.isEmpty()) {
            List<Instance> instances = objectManager.mappedChildren(service, Instance.class);
            for (Instance instance : instances) {
                String instanceName = getInstanceName(instance);
                if (instanceName != null) {
                    instanceLinksWithNames.put(instanceName, instanceName);
                }
            }
        }
        if (!instanceLinksWithNames.isEmpty()) {
            composeServiceData.put(ServiceDiscoveryConfigItem.EXTERNALLINKS.getComposeName(), instanceLinksWithNames);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> buildLaunchData(Service service) {
        Map<String, Object> data = getServiceDataAsMap(service);
        Map<String, Object> launchConfigItems = new HashMap<>();
        Set<Integer> volumesFromServices = new HashSet<>();
        for (String key : data.keySet()) {
            ServiceDiscoveryConfigItem item = ServiceDiscoveryConfigItem.getServiceConfigItemByRancherName(key);
            if (item != null && item.isLaunchConfigItem()) {
                // special handling for volumesFromService
                if (item.getRancherName().equals(ServiceDiscoveryConfigItem.VOLUMESFROMSERVICE.getRancherName())) {
                    List<Integer> serviceIds = (List<Integer>) data.get(key);
                    for (Integer serviceId : serviceIds) {
                        // get all instances for the service
                        List<? extends ServiceExposeMap> serviceInstancesMap = mapDao.findNonRemoved(
                                ServiceExposeMap.class,
                                Service.class, serviceId);
                        for (ServiceExposeMap map : serviceInstancesMap) {
                            volumesFromServices.add(map.getInstanceId().intValue());
                        }
                    }
                    continue;
                }
                launchConfigItems.put(key, data.get(key));
            }
        }

        if (!volumesFromServices.isEmpty()) {
            List<Integer> volumesFrom = (List<Integer>) launchConfigItems.get(ServiceDiscoveryConfigItem.VOLUMESFROM
                    .getRancherName());
            if (volumesFrom == null) {
                volumesFrom = new ArrayList<Integer>();
            }
            volumesFromServices.removeAll(volumesFrom);
            volumesFrom.addAll(volumesFromServices);
            launchConfigItems.put(ServiceDiscoveryConfigItem.VOLUMESFROM.getRancherName(), volumesFrom);
        }

        return launchConfigItems;
    }

    private String getInstanceName(Instance instance) {
        if (instance != null && instance.getRemoved() == null) {
            return instance.getUuid();
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getServiceDataAsMap(Service service) {
        Map<String, Object> originalData = new HashMap<>();
        originalData.putAll(DataUtils.getFields(service));
        Map<String, Object> data = new HashMap<>();
        data.putAll((Map<? extends String, ? extends Object>) originalData
                .get(ServiceDiscoveryConstants.FIELD_LAUNCH_CONFIG));
        originalData.remove(ServiceDiscoveryConstants.FIELD_LAUNCH_CONFIG);
        data.putAll(originalData);
        return data;
    }

    @Override
    public int[] getWeights(int size, int total) {
        int[] weights = new int[size];
        Integer sum = 0;
        int i = 0;
        while (true) {
            int value = total / size;
            if (sum + value > total) {
                weights[i - 1] = total - sum + value;
                break;
            }
            sum = sum + value;
            weights[i] = value;
            if (sum == 100) {
                break;
            }
            i++;
        }
        return weights;
    }

    @Override
    public long getServiceNetworkId(Service service) {
        Network network = ntwkDao.getNetworkForObject(service, NetworkConstants.KIND_HOSTONLY);
        if (network == null) {
            throw new RuntimeException(
                    "Unable to find a network to activate a service " + service);
        }
        long networkId = network.getId();
        return networkId;
    }

    protected String getServiceInstanceName(Service service, int order) {
        Environment env = objectManager.findOne(Environment.class, ENVIRONMENT.ID, service.getEnvironmentId());
        return String.format("%s_%s_%d", env.getName(), service.getName(), order + 1);
    }

    @Override
    public String getLoadBalancerName(Service service) {
        Environment env = objectManager.findOne(Environment.class, ENVIRONMENT.ID, service.getEnvironmentId());
        return String.format("%s_%s", env.getName(), service.getName());
    }

    @Override
    public Pair<Long, Long> getInstanceToServicePair(Instance instance) {
        List<? extends ServiceExposeMap> maps = objectManager.mappedChildren(
                objectManager.loadResource(Instance.class, instance.getId()),
                ServiceExposeMap.class);
        if (maps.isEmpty()) {
            // handle only instances that are the part of the service
            return null;
        }
        return new ImmutablePair<Long, Long>(instance.getId(), maps.get(0).getServiceId());
    }

    @Override
    public void activateService(Service service, int scale) {
        List<Instance> instancesToStart = new ArrayList<>();
        Map<String, Object> launchConfigData = buildLaunchData(service);
        Object registryCredentialId = launchConfigData.get(ServiceDiscoveryConfigItem.REGISTRYCREDENTIALID
                .getRancherName());
        Long imageId = getImage(String.valueOf(launchConfigData.get(InstanceConstants.FIELD_IMAGE_UUID)),
                registryCredentialId != null ? (Integer) registryCredentialId : null);
        List<Long> networkIds = getServiceNetworks(service);
        for (int i = 0; i < scale; i++) {
            Instance instance = createServiceInstance(service, i, launchConfigData, imageId, networkIds);
            instancesToStart.add(instance);
        }
        startServiceInstances(instancesToStart);
    }

    private void startServiceInstances(List<Instance> instancesToStart) {
        for (Instance instance : instancesToStart) {
            scheduleServiceInstanceStart(instance);
        }
        for (Instance instance : instancesToStart) {
            progress.checkPoint("start service instance " + instance.getName());
            instance = resourceMonitor.waitFor(instance, new ResourcePredicate<Instance>() {
                @Override
                public boolean evaluate(Instance obj) {
                    return InstanceConstants.STATE_RUNNING.equals(obj.getState());
                }
            });
        }
    }

    protected void scheduleServiceInstanceStart(final Instance instance) {
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
        long networkId = getServiceNetworkId(service);
        ntwkIds.add(networkId);
        return ntwkIds;
    }

    protected Long getImage(String imageUuid, Integer registryCredentialId) {
        Image image;
        try {
            image = storageService.registerRemoteImage(imageUuid);
            if (image == null) {
                return null;
            }
            if (registryCredentialId != null) {
                objectManager.setFields(image, IMAGE.REGISTRY_CREDENTIAL_ID, registryCredentialId);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to get image [" + imageUuid + "]");
        }

        return image == null ? null : image.getId();
    }

    protected Instance createServiceInstance(Service service, int i, Map<String, Object> launchConfigData, Long imageId,
            List<Long> networkIds) {
        String instanceName = getServiceInstanceName(service, i);
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
            properties.put(ServiceDiscoveryConstants.FIELD_SERVICE_ID, service.getId());
            Map<String, Object> props = objectManager.convertToPropertiesFor(Instance.class,
                    properties);
            instance = resourceDao.createAndSchedule(Instance.class, props);
        }

        return instance;
    }

    @Override
    public void activateLoadBalancerService(Service service, int scale) {
        LoadBalancer lb = objectManager.findOne(LoadBalancer.class, LOAD_BALANCER.SERVICE_ID, service.getId(),
                LOAD_BALANCER.REMOVED, null);

        List<Long> hostIds = (List<Long>) getLoadBalancerHostIds(service, scale, lb);
        for (Long hostId : hostIds) {
            lbService.addHostToLoadBalancer(lb, hostId);
        }
        waitForLbUpdate(lb, hostIds);
    }

    private void waitForLbUpdate(LoadBalancer lb, List<Long> hostIds) {
        for (Long hostId : hostIds) {
            LoadBalancerHostMap map = mapDao.findNonRemoved(LoadBalancerHostMap.class, Host.class, hostId,
                    LoadBalancer.class, lb.getId());
            map = resourceMonitor.waitFor(map, new ResourcePredicate<LoadBalancerHostMap>() {
                @Override
                public boolean evaluate(LoadBalancerHostMap obj) {
                    return obj != null && CommonStatesConstants.ACTIVE.equals(obj.getState());
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    protected List<Long> getLoadBalancerHostIds(Service service, int requestedScale, LoadBalancer lb) {
        // TODO - once the swarm is in, swarm cluster is going to be sent instead of the hostIds
        List<Host> availableHosts = objectManager.find(Host.class, HOST.ACCOUNT_ID, service.getAccountId(),
                HOST.STATE, CommonStatesConstants.ACTIVE, HOST.REMOVED, null);
        List<? extends LoadBalancerHostMap> existingHosts = mapDao.findNonRemoved(LoadBalancerHostMap.class,
                LoadBalancer.class, lb.getId());
        
        List<Long> availableHostIds = (List<Long>) CollectionUtils.collect(availableHosts,
                TransformerUtils.invokerTransformer("getId"));
        List<Long> existingHostIds = (List<Long>) CollectionUtils.collect(existingHosts,
                TransformerUtils.invokerTransformer("getHostId"));

        requestedScale = requestedScale - existingHostIds.size();
        availableHostIds.removeAll(existingHostIds);

        if (availableHostIds.size() < requestedScale) {
            throw new RuntimeException("Not enough hosts found  " + " to scale load balancer service "
                    + service.getName());
        }
        Collections.shuffle(availableHostIds);
        return availableHostIds.subList(0, requestedScale);
    }

    @Override
    public void deactivateLoadBalancerService(Service service) {
        LoadBalancer lb = objectManager.findOne(LoadBalancer.class, LOAD_BALANCER.SERVICE_ID, service.getId(),
                LOAD_BALANCER.REMOVED, null);
        if (lb == null) {
            return;
        }
        List<? extends LoadBalancerHostMap> maps = mapDao.findNonRemoved(LoadBalancerHostMap.class, LoadBalancer.class,
                lb.getId());
        for (LoadBalancerHostMap map : maps) {
            lbService.removeHostFromLoadBalancer(lb, map.getHostId());
        }
    }

    @Override
    public void deactivateService(Service service) {
        List<Instance> instances = objectManager.mappedChildren(service, Instance.class);
        if (!instances.isEmpty()) {
            stopServiceInstances(instances);
        }
    }

    private void stopServiceInstances(List<Instance> instances) {
        for (Instance instance : instances) {
            objectProcessManager.scheduleProcessInstance(InstanceConstants.PROCESS_STOP, instance, null);
        }
    }

    @Override
    public void scaleDownService(Service service, int requestedScale) {
        // on scale up, skip
        List<? extends Instance> serviceInstances = exposeMapDao.listNonRemovedInstancesForService(service.getId());
        int originalScale = serviceInstances.size();
        if (originalScale <= requestedScale) {
            return;
        }
        // remove instances
        int toRemove = originalScale - requestedScale;
        for (int i = originalScale - toRemove; i < originalScale; i++) {
            String instanceName = getServiceInstanceName(service, i);
            Instance instance = exposeMapDao.getServiceInstance(service.getId(), instanceName);
            if (instance != null) {
                removeServiceInstance(instance);
            }
        }
    }

    private void removeServiceInstance(Instance instance) {
        try {
            objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, instance, null);
        } catch (ProcessCancelException e) {
            Map<String, Object> data = new HashMap<>();
            data.put(InstanceConstants.REMOVE_OPTION, true);
            objectProcessManager.scheduleProcessInstance(InstanceConstants.PROCESS_STOP, instance,
                    data);
        }
    }

    @Override
    public void scaleDownLoadBalancerService(Service service, int requestedScale) {
        LoadBalancer lb = objectManager.findOne(LoadBalancer.class, LOAD_BALANCER.SERVICE_ID, service.getId(),
                LOAD_BALANCER.REMOVED, null);
        // on scale up, skip
        List<? extends LoadBalancerHostMap> existingHosts = mapDao.findNonRemoved(LoadBalancerHostMap.class,
                LoadBalancer.class, lb.getId());
        int originalScale = existingHosts.size();
        if (originalScale <= requestedScale) {
            return;
        }
        // remove hosts
        int toRemove = originalScale - requestedScale;
        for (int i = originalScale - toRemove; i < originalScale; i++) {
            LoadBalancerHostMap existingMap = existingHosts.get(i);
            lbService.removeHostFromLoadBalancer(lb, existingMap.getHostId());
        }
    }
}
