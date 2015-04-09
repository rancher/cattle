package io.cattle.platform.servicediscovery.service.impl;

import static io.cattle.platform.core.model.tables.EnvironmentTable.ENVIRONMENT;
import static io.cattle.platform.core.model.tables.InstanceTable.INSTANCE;
import static io.cattle.platform.core.model.tables.ServiceTable.SERVICE;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.dao.ServiceConsumeMapDao;
import io.cattle.platform.servicediscovery.resource.ServiceDiscoveryConfigItem;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.TransformerUtils;
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

    @Override
    public SimpleEntry<String, String> buildConfig(List<? extends Service> services) {
        Map<String, Object> dockerComposeData = createComposeData(services, true);
        Map<String, Object> rancherComposeData = createComposeData(services, false);

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        String dockerComposeDataStr = yaml.dump(dockerComposeData);
        String rancherComposeDataStr = yaml.dump(rancherComposeData);
        return new SimpleEntry<String, String>(dockerComposeDataStr, rancherComposeDataStr);
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
                    } else if (value != null) {
                        export = true;
                    }
                    if (export) {
                        composeServiceData.put(item.getComposeName(), value);
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

        // 3. now translate all the references services that are not being imported, to instance names
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
        Network network = ntwkDao.getNetworkForObject(service);
        if (network == null) {
            throw new RuntimeException(
                    "Unable to find a network to activate a service " + service);
        }
        long networkId = network.getId();
        return networkId;
    }

    @Override
    public String getInstanceName(Service service, int order) {
        Environment env = objectManager.findOne(Environment.class, ENVIRONMENT.ID, service.getEnvironmentId());
        return String.format("%s_%s_%d", env.getName(), service.getName(), order + 1);
    }
}
