package io.cattle.platform.servicediscovery.service.impl;

import static io.cattle.platform.core.model.tables.EnvironmentTable.ENVIRONMENT;
import static io.cattle.platform.core.model.tables.InstanceTable.INSTANCE;
import static io.cattle.platform.core.model.tables.LoadBalancerConfigTable.LOAD_BALANCER_CONFIG;
import static io.cattle.platform.core.model.tables.LoadBalancerListenerTable.LOAD_BALANCER_LISTENER;
import static io.cattle.platform.core.model.tables.LoadBalancerTable.LOAD_BALANCER;
import static io.cattle.platform.core.model.tables.ServiceTable.SERVICE;
import io.cattle.iaas.lb.service.LoadBalancerService;
import io.cattle.platform.allocator.service.AllocatorService;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.constants.NetworkConstants;
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
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.docker.constants.DockerNetworkConstants;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.dao.ServiceConsumeMapDao;
import io.cattle.platform.servicediscovery.api.dao.ServiceDao;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.resource.ServiceDiscoveryConfigItem;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.TransformerUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class ServiceDiscoveryServiceImpl implements ServiceDiscoveryService {

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

    @Inject
    AllocatorService allocatorService;

    @Inject
    ServiceDao serviceDao;

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
            List<String> launchConfigNames = getServiceLaunchConfigNames(service);
            for (String launchConfigName : launchConfigNames) {
                Map<String, Object> rancherServiceData = getServiceDataAsMap(service,
                        launchConfigName);
                Map<String, Object> composeServiceData = new HashMap<>();
                for (String rancherService : rancherServiceData.keySet()) {
                    translateRancherToCompose(forDockerCompose, rancherServiceData, composeServiceData, rancherService);
                }

                if (forDockerCompose) {
                    populateLinksForService(service, servicesToExportIds, composeServiceData);
                    populateNetworkForService(service, launchConfigName, composeServiceData);
                    populateVolumesForService(service, launchConfigName, composeServiceData);
                }

                data.put(
                        launchConfigName.equals(ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME) ? service
                                .getName() : launchConfigName, composeServiceData);
            }
        }
        return data;
    }


    private void populateNetworkForService(Service service,
            String launchConfigName, Map<String, Object> composeServiceData) {
        Object networkMode = composeServiceData.get(ServiceDiscoveryConfigItem.NETWORKMODE.getDockerName());
        if (networkMode != null) {
            if (networkMode.equals(DockerNetworkConstants.NETWORK_MODE_CONTAINER)) {
                Map<String, Object> serviceData = getLaunchConfigDataAsMap(service, launchConfigName);
                // network mode can be passed by container, or by service name, so check both
                // networkFromContainerId wins
                Integer targetContainerId = DataAccessor
                        .fieldInteger(service, DockerInstanceConstants.DOCKER_CONTAINER);
                if (targetContainerId != null) {
                    Instance instance = objectManager.loadResource(Instance.class, targetContainerId.longValue());
                    String instanceName = getInstanceName(instance);
                    composeServiceData.put(ServiceDiscoveryConfigItem.NETWORKMODE.getDockerName(),
                            DockerNetworkConstants.NETWORK_MODE_CONTAINER + ":" + instanceName);
                } else {
                    Object networkLaunchConfig = serviceData
                            .get(ServiceDiscoveryConstants.FIELD_NETWORK_LAUNCH_CONFIG);
                    if (networkLaunchConfig != null) {
                        composeServiceData.put(ServiceDiscoveryConfigItem.NETWORKMODE.getDockerName(),
                                DockerNetworkConstants.NETWORK_MODE_CONTAINER + ":" + networkLaunchConfig);
                    }
                }
            } else if (networkMode.equals(DockerNetworkConstants.NETWORK_MODE_MANAGED)) {
                composeServiceData.remove(ServiceDiscoveryConfigItem.NETWORKMODE.getDockerName());
            }
        }
    }


    protected void translateRancherToCompose(boolean forDockerCompose, Map<String, Object> rancherServiceData,
            Map<String, Object> composeServiceData, String rancherService) {
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
    public Map<String, Object> buildServiceInstanceLaunchData(Service service, Map<String, Object> deployParams, String launchConfigName) {
        Map<String, Object> serviceData = getLaunchConfigDataWLabelsUnion(service, launchConfigName);
        Map<String, Object> launchConfigItems = new HashMap<>();

        // 1. put all parameters retrieved through deployParams
        if (deployParams != null) {
            launchConfigItems.putAll(deployParams);
        }

        // 2. Get parameters defined on the service level (merge them with the ones defined in
        for (String key : serviceData.keySet()) {
            Object dataObj = serviceData.get(key);
            if (launchConfigItems.get(key) != null) {
                if (dataObj instanceof Map) {
                    // unfortunately, need to make an except for labels due to the merging aspect of the values
                    if (key.equalsIgnoreCase(InstanceConstants.FIELD_LABELS)) {
                        allocatorService.mergeLabels((Map<String, String>) launchConfigItems.get(key),
                                (Map<String, String>) dataObj);
                    } else {
                        ((Map<Object, Object>) dataObj).putAll((Map<Object, Object>) launchConfigItems.get(key));
                    }
                } else if (dataObj instanceof List) {
                    ((List<Object>) dataObj).addAll((List<Object>) launchConfigItems.get(key));
                }
            }
            if (dataObj != null) {
                launchConfigItems.put(key, dataObj);
            }
        }

        // 3. add extra parameters
        launchConfigItems.put("accountId", service.getAccountId());
        launchConfigItems.put("kind", InstanceConstants.KIND_CONTAINER);
        
        return launchConfigItems;
    }


    @SuppressWarnings("unchecked")
    @Override
    public Map<String, String> getServiceLabels(Service service) {
        List<String> launchConfigNames = getServiceLaunchConfigNames(service);
        Map<String, String> labelsStr = new HashMap<>();
        for (String launchConfigName : launchConfigNames) {
            Map<String, Object> data = getLaunchConfigDataAsMap(service, launchConfigName);
            Object labels = data.get(ServiceDiscoveryConfigItem.LABELS.getCattleName());
            if (labels != null) {
                allocatorService.mergeLabels((HashMap<String, String>) labels,
                        labelsStr);
            }
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

    private Map<String, Object> getServiceDataAsMap(Service service, String launchConfigName) {
        Map<String, Object> data = new HashMap<>();
        data.putAll(DataUtils.getFields(service));
        
        //1) remove launchConfig/secondaryConfig data
        Object launchConfig = data
                .get(ServiceDiscoveryConstants.FIELD_LAUNCH_CONFIG);
        if (launchConfig != null) {
            data.remove(ServiceDiscoveryConstants.FIELD_LAUNCH_CONFIG);
        }
        
        Object secondaryLaunchConfigs = data
                .get(ServiceDiscoveryConstants.FIELD_SECONDARY_LAUNCH_CONFIGS);
        if (secondaryLaunchConfigs != null) {
            data.remove(ServiceDiscoveryConstants.FIELD_SECONDARY_LAUNCH_CONFIGS);
        }
        // 2) populate launch config data
        data.putAll(getLaunchConfigDataWLabelsUnion(service, launchConfigName));
        
        return data;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public List<String> getServiceLaunchConfigNames(Service service) {
        Map<String, Object> originalData = new HashMap<>();
        originalData.putAll(DataUtils.getFields(service));
        List<String> launchConfigNames = new ArrayList<>();

        // put the primary config in
        launchConfigNames.add(ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME);

        // put the secondary configs in
        Object secondaryLaunchConfigs = originalData
                .get(ServiceDiscoveryConstants.FIELD_SECONDARY_LAUNCH_CONFIGS);
        if (secondaryLaunchConfigs != null) {
            for (Map<String, Object> secondaryLaunchConfig : (List<Map<String, Object>>) secondaryLaunchConfigs) {
                launchConfigNames.add(String.valueOf(secondaryLaunchConfig.get("name")));
            }
        }

        return launchConfigNames;
    }
    

    private Map<String, Object> getLaunchConfigDataWLabelsUnion(Service service, String launchConfigName) {
        Map<String, Object> launchConfigData = new HashMap<>();
        
        // 1) get launch config data from the list of primary/secondary
        launchConfigData.putAll(getLaunchConfigDataAsMap(service, launchConfigName));
        
        // 2. remove labels, and request the union
        launchConfigData.remove(InstanceConstants.FIELD_LABELS);
        launchConfigData.put(InstanceConstants.FIELD_LABELS, getServiceLabels(service));

        return launchConfigData;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> getLaunchConfigDataAsMap(Service service, String launchConfigName) {
        if (launchConfigName == null) {
            launchConfigName = ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME;
        }
        Map<String, Object> launchConfigData = new HashMap<>();
        if (launchConfigName.equalsIgnoreCase(ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME)) {
            launchConfigData = (Map<String, Object>) DataAccessor.fields(service)
                    .withKey(ServiceDiscoveryConstants.FIELD_LAUNCH_CONFIG).withDefault(Collections.EMPTY_MAP)
                    .as(Map.class);
        } else {
            List<Map<String, Object>> secondaryLaunchConfigs = DataAccessor.fields(service)
                    .withKey(ServiceDiscoveryConstants.FIELD_SECONDARY_LAUNCH_CONFIGS)
                    .withDefault(Collections.EMPTY_LIST).as(
                            List.class);
            for (Map<String, Object> secondaryLaunchConfig : secondaryLaunchConfigs) {
                if (secondaryLaunchConfig.get("name").toString().equalsIgnoreCase(launchConfigName)) {
                    launchConfigData = secondaryLaunchConfig;
                    break;
                }
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.putAll(launchConfigData);

        Object labels = data.get(ServiceDiscoveryConfigItem.LABELS.getCattleName());
        if (labels != null) {
            Map<String, String> labelsMap = new HashMap<String, String>();
            labelsMap.putAll((Map<String, String>) labels);

            // overwrite with a copy of the map
            data.put(ServiceDiscoveryConfigItem.LABELS.getCattleName(), labelsMap);
        }
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
                    "Unable to find a network to activate a service " + service.getId());
        }
        long networkId = network.getId();
        return networkId;
    }


    private boolean isServiceGeneratedName(Service service, Instance serviceInstance) {
        Environment env = objectManager.findOne(Environment.class, ENVIRONMENT.ID, service.getEnvironmentId());
        return serviceInstance.getName().startsWith(String.format("%s_%s", env.getName(), service.getName()));
    }

    @Override
    public String generateServiceInstanceName(Service service, String launchConfigName, int finalOrder) {
        Environment env = objectManager.findOne(Environment.class, ENVIRONMENT.ID, service.getEnvironmentId());
        String configName = launchConfigName == null
                || launchConfigName.equals(ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME) ? ""
                : launchConfigName + "_";
        String name = String.format("%s_%s_%s%d", env.getName(), service.getName(), configName, finalOrder);
        return name;
    }

    @Override
    public List<Integer> getServiceInstanceUsedOrderIds(Service service, String launchConfigName) {
        Environment env = objectManager.findOne(Environment.class, ENVIRONMENT.ID, service.getEnvironmentId());
        // get all existing instances to check if the name is in use by the instance of the same service
        List<Integer> usedIds = new ArrayList<>();
        // list all the instances
        List<? extends ServiceExposeMap> instanceServiceMaps = exposeMapDao.getNonRemovedServiceInstanceMap(service
                .getId());
        
        for (ServiceExposeMap instanceServiceMap : instanceServiceMaps) {
            Instance instance = objectManager.loadResource(Instance.class, instanceServiceMap.getInstanceId());
            if (isServiceGeneratedName(service, instance)) {
                
                String configName = launchConfigName == null
                        || launchConfigName.equals(ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME) ? ""
                        : launchConfigName + "_";
                
                String id = instance.getName().replace(String.format("%s_%s_%s", env.getName(), service.getName(), configName), "");
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
        Map<String, Object> launchConfigData = getLaunchConfigDataAsMap(service, null);
        createListeners(service, lbConfig, launchConfigData);

        // 3. create a load balancer
        createLoadBalancer(service, lbName, lbConfig);
    }

    private void createLoadBalancer(Service service, String lbName, LoadBalancerConfig lbConfig) {
        LoadBalancer lb = objectManager.findOne(LoadBalancer.class, LOAD_BALANCER.SERVICE_ID, service.getId(),
                LOAD_BALANCER.REMOVED, null, LOAD_BALANCER.ACCOUNT_ID, service.getAccountId());
        if (lb == null) {
            Map<String, Object> launchConfigData = getLaunchConfigDataAsMap(service, null);
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

    public List<Service> getServicesFor(Object obj) {
        List<? extends Service> dbResult = null;
        if (obj instanceof Instance) {
            dbResult = serviceDao.findServicesFor((Instance) obj);
        }

        if (dbResult == null) {
            return Collections.emptyList();
        }

        return new ArrayList<>(dbResult);
    }

    @SuppressWarnings("unchecked")
    private void populateVolumesForService(Service service, String launchConfigName,
            Map<String, Object> composeServiceData) {
        List<String> namesCombined = new ArrayList<>();
        List<String> launchConfigNames = new ArrayList<>();
        Map<String, Object> launchConfigData = getLaunchConfigDataAsMap(service, launchConfigName);
        Object dataVolumesLaunchConfigs = launchConfigData.get(
                ServiceDiscoveryConstants.FIELD_DATA_VOLUMES_LAUNCH_CONFIG);

        if (dataVolumesLaunchConfigs != null) {
            launchConfigNames.addAll((List<String>) dataVolumesLaunchConfigs);
        }

        // 1. add launch config names
        namesCombined.addAll(launchConfigNames);

        // 2. add instance names if specified
        List<? extends Integer> instanceIds = (List<? extends Integer>) launchConfigData
                .get(DockerInstanceConstants.FIELD_VOLUMES_FROM);

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

        if (!namesCombined.isEmpty()) {
            composeServiceData.put(ServiceDiscoveryConfigItem.VOLUMESFROM.getDockerName(), namesCombined);
        }
    }

    @Override
    public Object getLaunchConfigObject(Service service, String launchConfigName, String objectName) {
        Map<String, Object> serviceData = getLaunchConfigDataAsMap(service, launchConfigName);
        return serviceData.get(objectName);
    }

}
