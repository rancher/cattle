package io.cattle.platform.servicediscovery.service.impl;

import static io.cattle.platform.core.model.tables.EnvironmentTable.ENVIRONMENT;
import static io.cattle.platform.core.model.tables.InstanceTable.INSTANCE;
import static io.cattle.platform.core.model.tables.LoadBalancerConfigTable.LOAD_BALANCER_CONFIG;
import static io.cattle.platform.core.model.tables.LoadBalancerListenerTable.LOAD_BALANCER_LISTENER;
import static io.cattle.platform.core.model.tables.LoadBalancerTable.LOAD_BALANCER;
import static io.cattle.platform.core.model.tables.ServiceTable.SERVICE;
import io.cattle.iaas.lb.service.LoadBalancerService;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerConfig;
import io.cattle.platform.core.model.LoadBalancerListener;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.dao.ServiceConsumeMapDao;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.resource.ServiceDiscoveryConfigItem;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    ObjectProcessManager objectProcessManager;

    @Inject
    LoadBalancerService lbService;

    @Inject
    ServiceExposeMapDao exposeMapDao;

    @Inject
    JsonMapper jsonMapper;

    @Override
    public SimpleEntry<String, String> buildComposeConfig(List<? extends Service> services) {
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
                ServiceDiscoveryConfigItem item = ServiceDiscoveryConfigItem.getServiceConfigItemByCattleName(rancherService);
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
                        // for every lookup, do transform
                        Object formattedValue = null;
                        for (RancherConfigToComposeFormatter formatter : formatters) {
                            formattedValue = formatter.format(item, value);
                            if (formattedValue != null) {
                                break;
                            }
                        }
                        if (formattedValue != null) {
                            composeServiceData.put(item.getDockerName().toLowerCase(), formattedValue);
                        } else {
                            composeServiceData.put(item.getDockerName().toLowerCase(), value);
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
                ServiceDiscoveryConfigItem.VOLUMESFROMSERVICE.getCattleName());

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
                ServiceDiscoveryConfigItem.VOLUMESFROM.getCattleName());

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
            composeServiceData.put(ServiceDiscoveryConfigItem.VOLUMESFROM.getDockerName(), namesCombined);
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
            composeServiceData.put(ServiceDiscoveryConfigItem.LINKS.getDockerName(), serviceLinks);
        }
        populateExternalLinksForService(service, composeServiceData, externalLinksServices);
    }

    @SuppressWarnings("unchecked")
    private void populateExternalLinksForService(Service service, Map<String, Object> composeServiceData,
            List<Service> externalLinksServices) {

        Map<String, String> instanceLinksWithNames = new LinkedHashMap<String, String>();
        Map<String, Object> instanceLinksWithIds = (Map<String, Object>) composeServiceData
                .get(ServiceDiscoveryConfigItem.EXTERNALLINKS
                .getDockerName());
        if (instanceLinksWithIds != null) {
            for (String linkName : instanceLinksWithIds.keySet()) {
                Instance instance = objectManager.findOne(Instance.class, INSTANCE.ID,
                        instanceLinksWithIds.get(linkName), INSTANCE.REMOVED,
                        null);
                String instanceName = getInstanceName(instance);
                if (instanceName != null) {
                    instanceLinksWithNames.put(instanceName, linkName);
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
            composeServiceData.put(ServiceDiscoveryConfigItem.EXTERNALLINKS.getDockerName(), instanceLinksWithNames);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> buildLaunchData(Service service, Map<String, String> labels, String instanceName, List<Integer> volumesFromInstanceIds) {
        Map<String, Object> data = getServiceDataAsMap(service);
        Map<String, Object> launchConfigItems = new HashMap<>();

        // convert compose to rancher name
        for (String key : data.keySet()) {
            ServiceDiscoveryConfigItem item = ServiceDiscoveryConfigItem.getServiceConfigItemByCattleName(key);
            if (item != null && item.isLaunchConfigItem()) {
                launchConfigItems.put(key, data.get(key));
            }
        }
        //populate name
        if (instanceName != null) {
            launchConfigItems.put("name", instanceName);
        }
        
        // populate labels
        launchConfigItems.put(ServiceDiscoveryConfigItem.LABELS.getCattleName(), labels);

        // process volumes from
        if (volumesFromInstanceIds != null && !volumesFromInstanceIds.isEmpty()) {
            List<Integer> volumesFrom = (List<Integer>) launchConfigItems.get(ServiceDiscoveryConfigItem.VOLUMESFROM
                    .getCattleName());
            if (volumesFrom == null) {
                volumesFrom = new ArrayList<Integer>();
            }
            volumesFrom.addAll(volumesFromInstanceIds);
            launchConfigItems.put(ServiceDiscoveryConfigItem.VOLUMESFROM.getCattleName(), volumesFrom);
        }

        return launchConfigItems;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, String> getServiceLabels(Service service) {
        Map<String, Object> data = getServiceDataAsMap(service);
        Object labels = data.get(ServiceDiscoveryConfigItem.LABELS.getCattleName());
        Map<String, String> labelsStr = new HashMap<>();
        if (labels != null) {
            labelsStr.putAll((HashMap<String, String>) labels);
        }
        return labelsStr;
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
        int[] weights = size == 0 ? new int[1] : new int[size];
        if (size == 0) {
            weights[0] = total;
            return weights;
        }

        Integer sum = 0;
        int i = 0;
        while (true) {
            int value = total / size;
            if (sum + value > total || i == weights.length) {
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

    protected long getServiceNetworkId(Service service) {
        Network network = ntwkDao.getNetworkForObject(service, NetworkConstants.KIND_HOSTONLY);
        if (network == null) {
            throw new RuntimeException(
                    "Unable to find a network to activate a service " + service);
        }
        long networkId = network.getId();
        return networkId;
    }


    private boolean isServiceGeneratedName(Service service, Instance serviceInstance) {
        Environment env = objectManager.findOne(Environment.class, ENVIRONMENT.ID, service.getEnvironmentId());
        return serviceInstance.getName().startsWith(String.format("%s_%s", env.getName(), service.getName()));
    }

    @Override
    public String generateServiceInstanceName(Service service, int finalOrder) {
        Environment env = objectManager.findOne(Environment.class, ENVIRONMENT.ID, service.getEnvironmentId());
        String name = String.format("%s_%s_%d", env.getName(), service.getName(), finalOrder);
        return name;
    }

    @Override
    public List<Integer> getServiceInstanceUsedOrderIds(Service service) {
        Environment env = objectManager.findOne(Environment.class, ENVIRONMENT.ID, service.getEnvironmentId());
        // get all existing instances to check if the name is in use by the instance of the same service
        List<Integer> usedIds = new ArrayList<>();
        // list all the instances
        List<? extends ServiceExposeMap> instanceServiceMaps = exposeMapDao.getNonRemovedServiceInstanceMap(service
                .getId());
        
        for (ServiceExposeMap instanceServiceMap : instanceServiceMaps) {
            Instance instance = objectManager.loadResource(Instance.class, instanceServiceMap.getInstanceId());
            if (isServiceGeneratedName(service, instance)) {
                String id = instance.getName().replace(String.format("%s_%s_", env.getName(), service.getName()), "");
                if (id.matches("\\d+")) {
                    usedIds.add(Integer.valueOf(id));
                }
            }
        }
        return usedIds;
    }

    protected String getLoadBalancerName(Service service) {
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
    public List<Long> getServiceNetworkIds(Service service) {
        List<Long> ntwkIds = new ArrayList<>();
        long networkId = getServiceNetworkId(service);
        ntwkIds.add(networkId);
        return ntwkIds;
    }

    @Override
    public void cleanupLoadBalancerService(Service service) {
        // 1) remove load balancer
        LoadBalancer lb = objectManager.findOne(LoadBalancer.class, LOAD_BALANCER.SERVICE_ID, service.getId(),
                LOAD_BALANCER.REMOVED, null);
        if (lb != null) {
            objectProcessManager.scheduleProcessInstance(LoadBalancerConstants.PROCESS_LB_REMOVE, lb, null);
        }
    }

    @Override
    public void removeServiceMaps(Service service) {
        // 1. remove all maps to the services consumed by service specified
        for (ServiceConsumeMap map : consumeMapDao.findConsumedMapsToRemove(service.getId())) {
            objectProcessManager.scheduleProcessInstance(ServiceDiscoveryConstants.PROCESS_SERVICE_CONSUME_MAP_REMOVE,
                    map, null);
        }

        // 2. remove all maps to the services consuming service specified
        for (ServiceConsumeMap map : consumeMapDao.findConsumingMapsToRemove(service.getId())) {
            objectProcessManager.scheduleProcessInstance(ServiceDiscoveryConstants.PROCESS_SERVICE_CONSUME_MAP_REMOVE,
                    map, null);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void createLoadBalancerService(Service service) {
        String lbName = getLoadBalancerName(service);
        // 1. create load balancer config
        Map<String, Object> lbConfigData = (Map<String, Object>) DataAccessor.field(service,
                ServiceDiscoveryConstants.FIELD_LOAD_BALANCER_CONFIG,
                jsonMapper,
                Map.class);

        if (lbConfigData == null) {
            lbConfigData = new HashMap<String, Object>();
        }

        LoadBalancerConfig lbConfig = createDefaultLoadBalancerConfig(lbName, lbConfigData,
                service);

        // 2. add listeners to the config based on the ports info
        Map<String, Object> launchConfigData = buildLaunchData(service, new HashMap<String, String>(), null, null);
        createListeners(service, lbConfig, launchConfigData);

        // 3. create a load balancer
        createLoadBalancer(service, lbName, lbConfig);
    }

    private void createLoadBalancer(Service service, String lbName, LoadBalancerConfig lbConfig) {
        LoadBalancer lb = objectManager.findOne(LoadBalancer.class, LOAD_BALANCER.SERVICE_ID, service.getId(),
                LOAD_BALANCER.REMOVED, null, LOAD_BALANCER.ACCOUNT_ID, service.getAccountId());
        if (lb == null) {
            Map<String, Object> launchConfigData = buildLaunchData(service, new HashMap<String, String>(), null, null);
            Map<String, Object> data = new HashMap<>();
            data.put("name", lbName);
            data.put(LoadBalancerConstants.FIELD_LB_CONFIG_ID, lbConfig.getId());
            data.put(LoadBalancerConstants.FIELD_LB_SERVICE_ID, service.getId());
            data.put(LoadBalancerConstants.FIELD_LB_NETWORK_ID, getServiceNetworkId(service));
            data.put(
                    LoadBalancerConstants.FIELD_LB_INSTANCE_IMAGE_UUID,
                    launchConfigData.get(InstanceConstants.FIELD_IMAGE_UUID));
            data.put(
                    LoadBalancerConstants.FIELD_LB_INSTANCE_URI_PREDICATE,
                    DataAccessor.fields(service).withKey(LoadBalancerConstants.FIELD_LB_INSTANCE_URI_PREDICATE)
                            .withDefault("delegate:///").as(
                                    String.class));
            data.put("accountId", service.getAccountId());
            lb = objectManager.create(LoadBalancer.class, data);
        }

        objectProcessManager.executeProcess(LoadBalancerConstants.PROCESS_LB_CREATE, lb, null);
    }

    private LoadBalancerConfig createDefaultLoadBalancerConfig(String defaultName,
            Map<String, Object> lbConfigData, Service service) {
        String name = lbConfigData.get("name") == null ? defaultName : lbConfigData.get("name")
                .toString();
        LoadBalancerConfig lbConfig = objectManager.findOne(LoadBalancerConfig.class,
                LOAD_BALANCER_CONFIG.REMOVED, null,
                LOAD_BALANCER_CONFIG.ACCOUNT_ID, service.getAccountId(),
                LOAD_BALANCER_CONFIG.SERVICE_ID, service.getId());

        if (lbConfig == null) {
            lbConfigData.put("accountId", service.getAccountId());
            lbConfigData.put("name", name);
            lbConfigData.put("serviceId", service.getId());
            lbConfig = objectManager.create(LoadBalancerConfig.class, lbConfigData);
        }
        objectProcessManager.executeProcess(LoadBalancerConstants.PROCESS_LB_CONFIG_CREATE, lbConfig, null);
        return lbConfig;
    }

    @SuppressWarnings("unchecked")
    private void createListeners(Service service, LoadBalancerConfig lbConfig, Map<String, Object> launchConfigData) {
        // 1. create listeners
        List<String> portDefs = (List<String>) launchConfigData.get(InstanceConstants.FIELD_PORTS);
        if (portDefs == null || portDefs.isEmpty()) {
            return;
        }

        Map<Integer, LoadBalancerListener> listeners = new HashMap<>();

        for (String port : portDefs) {
            PortSpec spec = new PortSpec(port);
            if (!port.contains("tcp")) {
                // default to http unless defined otherwise in the compose file
                spec.setProtocol("http");
            }

            if (listeners.containsKey(spec.getPrivatePort())) {
                continue;
            }

            Integer publicPort = spec.getPublicPort();
            int privatePort = spec.getPrivatePort();
            if (publicPort == null) {
                publicPort = privatePort;
            }

            LoadBalancerListener listenerObj = objectManager.findOne(LoadBalancerListener.class,
                    LOAD_BALANCER_LISTENER.SERVICE_ID, service.getId(),
                    LOAD_BALANCER_LISTENER.SOURCE_PORT, publicPort,
                    LOAD_BALANCER_LISTENER.TARGET_PORT, privatePort,
                    LOAD_BALANCER_LISTENER.REMOVED, null,
                    LOAD_BALANCER_LISTENER.ACCOUNT_ID, service.getAccountId());

            if (listenerObj == null) {
                listenerObj = objectManager.create(LoadBalancerListener.class,
                        LOAD_BALANCER_LISTENER.NAME, getLoadBalancerName(service) + "_" + publicPort,
                        LOAD_BALANCER_LISTENER.ACCOUNT_ID,
                        service.getAccountId(), LOAD_BALANCER_LISTENER.SOURCE_PORT, publicPort,
                        LOAD_BALANCER_LISTENER.TARGET_PORT, privatePort,
                        LOAD_BALANCER_LISTENER.SOURCE_PROTOCOL, spec.getProtocol(),
                        LOAD_BALANCER_LISTENER.TARGET_PROTOCOL,
                        spec.getProtocol(),
                        LoadBalancerConstants.FIELD_LB_LISTENER_ALGORITHM, "roundrobin",
                        LOAD_BALANCER_LISTENER.ACCOUNT_ID, service.getAccountId(),
                        LOAD_BALANCER_LISTENER.SERVICE_ID, service.getId());
            }
            objectProcessManager.executeProcess(LoadBalancerConstants.PROCESS_LB_LISTENER_CREATE, listenerObj, null);

            listeners.put(listenerObj.getTargetPort(), listenerObj);
        }

        for (LoadBalancerListener listener : listeners.values()) {
            lbService.addListenerToConfig(lbConfig, listener.getId());
        }
    }

    @Override
    public List<? extends Service> listEnvironmentServices(long environmentId) {
        return objectManager.find(Service.class, SERVICE.ENVIRONMENT_ID, environmentId, SERVICE.REMOVED,
                null);
    }

}
