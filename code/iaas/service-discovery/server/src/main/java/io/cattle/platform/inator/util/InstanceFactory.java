package io.cattle.platform.inator.util;

import static io.cattle.platform.core.model.tables.VolumeTable.*;
import static io.cattle.platform.core.model.tables.VolumeTemplateTable.*;

import io.cattle.platform.allocator.constraint.ContainerLabelAffinityConstraint;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceIndex;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeTemplate;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.inator.wrapper.DeploymentUnitWrapper;
import io.cattle.platform.inator.wrapper.ServiceRevisionWrapper;
import io.cattle.platform.inator.wrapper.StackWrapper;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.lock.StackVolumeLock;
import io.cattle.platform.util.exception.DeploymentUnitAllocateException;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class InstanceFactory {

    public static Map<String, Object> createInstanceData(Map<String, Object> launchConfig,
            StackWrapper stack,
            ServiceRevisionWrapper service,
            DeploymentUnitWrapper unit,
            String launchConfigName,
            ServiceIndex serviceIndex,
            String instanceName) {

        Map<String, Object> fields = new HashMap<>(launchConfig);
        addAdditionalFields(fields, stack, service, unit, launchConfigName, serviceIndex);
        addDns(fields, stack, service);
        addHostnameOverride(instanceName, fields);
        //createNamedVolumes(stack, service, unit, launchConfigName, deploymentUnitData);
        //removeNamedVolumesFromLaunchConfig(deploymentUnitInstanceData, deploymentUnitInstanceData);

        fields.put(ObjectMetaDataManager.NAME_FIELD, instanceName);
        fields.put(ServiceConstants.FIELD_SYSTEM, ServiceConstants.isSystem(stack.getInternal()));

        return fields;
    }


    private static void addHostnameOverride(String instanceName, Map<String, Object> fields) {
        Map<String, String> labels = getLabels(fields);
        if (!"container_name".equalsIgnoreCase(labels.get(ServiceConstants.LABEL_OVERRIDE_HOSTNAME))) {
            return;
        }

        Object domainName = fields.get(DockerInstanceConstants.FIELD_DOMAIN_NAME);
        String overrideName = getOverrideHostName(domainName, instanceName);
        fields.put(InstanceConstants.FIELD_HOSTNAME, overrideName);
    }

    protected static Map<String, String> getLabels(Map<String, Object> fields) {
        return CollectionUtils.toMap(fields.get(InstanceConstants.FIELD_LABELS));
    }

    protected static void setLabels(Map<String, Object> fields, Map<String, String> labels) {
        fields.put(InstanceConstants.FIELD_LABELS, labels);
    }

    protected static void addDns(Map<String, Object> fields, StackWrapper stack, ServiceRevisionWrapper service) {
        Map<String, String> labels = CollectionUtils.toMap(fields.get(InstanceConstants.FIELD_LABELS));
        if ("false".equalsIgnoreCase(labels.get(SystemLabels.LABEL_USE_RANCHER_DNS))) {
            return;
        }
        fields.put(DockerInstanceConstants.FIELD_DNS_SEARCH, getSearchDomains(stack, service));
    }

    protected static List<String> getSearchDomains(StackWrapper stack, ServiceRevisionWrapper service) {
        String stackNamespace = ServiceUtil.getStackNamespace(stack.getName());
        String serviceNamespace = ServiceUtil.getServiceNamespace(stack.getName(), service.getName());
        return Arrays.asList(stackNamespace, serviceNamespace);
    }

    public static void createNamedVolumes(Map<String, Object> launchConfig, Stack stack, Service service, DeploymentUnit unit, String launchConfigName,
            Map<String, Object> deploymentUnitParams) {
        List<String> namedVolumes = getNamedVolumes(launchConfig);
        List<String> internalVolumes = new ArrayList<>();
        Map<String, Long> volumeMounts = new HashMap<>();
        Iterator<String> it = namedVolumes.iterator();
        while (it.hasNext()) {
            String namedVolume = it.next();
            getOrCreateVolume(stack, service, unit, namedVolume, volumeMounts, internalVolumes);
            if (!internalVolumes.contains(namedVolume)) {
                it.remove();
            }
        }

        deploymentUnitParams.put(ServiceConstants.FIELD_INTERNAL_VOLUMES, namedVolumes);
        deploymentUnitParams.put(InstanceConstants.FIELD_DATA_VOLUME_MOUNTS, volumeMounts);
    }

    protected static void getOrCreateVolume(Stack stack, Service service, DeploymentUnit unit, String volumeName,
            Map<String, Long> volumeMounts,
            List<String> internalVolumes) {
        if (unit.getServiceIndex() == null) {
            return;
        }
        String[] splitted = volumeName.split(":");
        String volumeNamePostfix = splitted[0];
        String volumePath = volumeName.replaceFirst(splitted[0] + ":", "");

        final VolumeTemplate template = objectManager.findOne(VolumeTemplate.class, VOLUME_TEMPLATE.ACCOUNT_ID,
                service.getAccountId(), VOLUME_TEMPLATE.REMOVED, null, VOLUME_TEMPLATE.NAME, splitted[0],
                VOLUME_TEMPLATE.STACK_ID, stack.getId());
        if (template == null) {
            return;
        }

        Volume volume = null;
        if (template.getExternal()) {
            // external volume should exist, otherwise fail
            volume = objectManager.findAny(Volume.class, VOLUME.ACCOUNT_ID, service.getAccountId(),
                    VOLUME.REMOVED, null, VOLUME.NAME, template.getName());
            if (volume == null) {
                throw new DeploymentUnitAllocateException("Failed to locate volume for for deployment unit ["
                        + unit.getUuid() + "]", null, unit);
            }
            return;
        } else {
            final String postfix = io.cattle.platform.util.resource.UUID.randomUUID().toString();
            if (template.getPerContainer()) {
                String name = stack.getName() + "_" + volumeNamePostfix + "_" + unit.getServiceIndex() + "_"
                        + unit.getUuid() + "_";
                // append 5 random chars
                List<? extends Volume> volumes = objectManager
                        .find(Volume.class, VOLUME.ACCOUNT_ID, service.getAccountId(),
                                VOLUME.REMOVED, null, VOLUME.VOLUME_TEMPLATE_ID, template.getId(), VOLUME.STACK_ID,
                                stack.getId(), VOLUME.DEPLOYMENT_UNIT_ID, unit.getId());
                for (Volume vol : volumes) {
                    if (vol.getName().startsWith(name)) {
                        volume = vol;
                        break;
                    }
                }
                if (volume == null) {
                    volume = createVolume(service, unit, template, name + postfix.substring(0, 5));
                }
            } else {
                final String name = stack.getName() + "_" + volumeNamePostfix + "_";
                volume = lockManager.lock(new StackVolumeLock(stack, name), new LockCallback<Volume>() {
                    @Override
                    public Volume doWithLock() {
                        Volume existing = null;
                        List<? extends Volume> volumes = objectManager
                                .find(Volume.class, VOLUME.ACCOUNT_ID, service.getAccountId(),
                                        VOLUME.REMOVED, null, VOLUME.VOLUME_TEMPLATE_ID, template.getId(),
                                        VOLUME.STACK_ID,
                                        stack.getId());
                        for (Volume vol : volumes) {
                            if (vol.getName().startsWith(name)) {
                                existing = vol;
                                break;
                            }
                        }
                        if (existing != null) {
                            return existing;
                        }
                        return createVolume(service, unit, template, new String(name + postfix.substring(0, 5)));
                    }
                });
            }
        }
        volumeMounts.put(volumePath, volume.getId());
        internalVolumes.add(volumeName);
    }

    private static Volume createVolume(Service service, DeploymentUnit unit, VolumeTemplate template, String name) {
        Map<String, Object> params = new HashMap<>();
        if (template.getPerContainer()) {
            params.put(ServiceConstants.FIELD_DEPLOYMENT_UNIT_ID, unit.getId());
        }
        params.put("name", name);
        params.put("accountId", service.getAccountId());
        params.put(InstanceConstants.FIELD_STACK_ID, service.getStackId());
        params.put(ServiceConstants.FIELD_VOLUME_TEMPLATE_ID, template.getId());
        params.put(VolumeConstants.FIELD_VOLUME_DRIVER_OPTS,
                DataAccessor.fieldMap(template, VolumeConstants.FIELD_VOLUME_DRIVER_OPTS));
        params.put(VolumeConstants.FIELD_VOLUME_DRIVER, template.getDriver());
        return resourceDao.createAndSchedule(Volume.class, params);
    }

    @SuppressWarnings("unchecked")
    protected static List<String> getNamedVolumes(Map<String, Object> launchConfig) {
        return CollectionUtils.toList(launchConfig.get(InstanceConstants.FIELD_DATA_VOLUMES)).stream()
            .filter((o) -> isNamedVolume(o.toString()))
            .map((o) -> o.toString())
            .collect(Collectors.toList());
    }

    protected static boolean isNamedVolume(String volumeName) {
        String[] splitted = volumeName.split(":");
        if (splitted.length < 2) {
            return false;
        }
        if (splitted[0].contains("/")) {
            return false;
        }
        return true;
    }


    private static String getOverrideHostName(Object domainName, String instanceName) {
        if (instanceName == null || instanceName.length() <= 64) {
            return instanceName;
        }

        // legacy code - to support old data where service suffix object wasn't created
        String serviceSuffix = ServiceConstants.getServiceIndexFromInstanceName(instanceName);
        String serviceSuffixWDivider = instanceName.substring(instanceName.lastIndexOf(serviceSuffix) - 1);
        int truncateIndex = 64 - serviceSuffixWDivider.length();
        if (domainName != null) {
            truncateIndex = truncateIndex - domainName.toString().length() - 1;
        }
        return instanceName.substring(0, truncateIndex) + serviceSuffixWDivider;
    }

    protected static void addAdditionalFields(Map<String, Object> fields, StackWrapper stack, ServiceRevisionWrapper serviceRevision, DeploymentUnitWrapper unit,
            String launchConfigName, ServiceIndex serviceIndex) {
        fields.put(InstanceConstants.FIELD_DEPLOYMENT_UNIT_UUID, unit.getUuid());
        fields.put(InstanceConstants.FIELD_DEPLOYMENT_UNIT_ID, unit.getId());
        fields.put(InstanceConstants.FIELD_STACK_ID, stack.getId());
        fields.put(ObjectMetaDataManager.ACCOUNT_FIELD, stack.getAccountId());
        if (!fields.containsKey(ObjectMetaDataManager.KIND_FIELD)) {
            fields.put(ObjectMetaDataManager.KIND_FIELD, InstanceConstants.KIND_CONTAINER);
        }

        if (serviceRevision != null) {
            addServiceFields(fields, stack, serviceRevision, unit, launchConfigName, serviceIndex);
        }

        if (serviceIndex != null) {
            addServiceIndexFields(fields, serviceIndex);
        }
    }

    protected static void addServiceFields(Map<String, Object> fields, StackWrapper stack, ServiceRevisionWrapper serviceRevision, DeploymentUnitWrapper unit,
            String launchConfigName, ServiceIndex serviceIndex) {
        Map<String, String> labels = createServiceLabels(fields, serviceRevision, stack, unit, launchConfigName);
        addLabels(fields, labels);
        fields.put(InstanceConstants.FIELD_SERVICE_ID, serviceRevision.getServiceId());
        // TODO REMOVE THIS MAYBE
        fields.put(InstanceConstants.FIELD_SERVICE_REVISION_ID, serviceRevision.getId());
    }

    protected static void addLabels(Map<String, Object> fields, Map<String, String> labels) {
        Map<String, String> toLabels = getLabels(fields);
        toLabels.putAll(labels);
        setLabels(fields, toLabels);
    }

    protected static void addServiceIndexFields(Map<String, Object> fields, ServiceIndex serviceIndex) {
        fields.put(InstanceConstants.FIELD_SERVICE_INSTANCE_SERVICE_INDEX_ID, serviceIndex.getId());
        fields.put(InstanceConstants.FIELD_SERVICE_INSTANCE_SERVICE_INDEX, serviceIndex.getServiceIndex());
        fields.put(InstanceConstants.FIELD_ALLOCATED_IP_ADDRESS, serviceIndex.getAddress());
    }

    protected static Map<String, String> createServiceLabels(Map<String, Object> fields, ServiceRevisionWrapper service, StackWrapper stack,
            DeploymentUnitWrapper unit, String launchConfigName) {
        Map<String, String> labels = new HashMap<>();
        String serviceName = service.getName();
        String envName = stack.getName();

        if (!ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME.equals(launchConfigName)) {
            serviceName = serviceName + '/' + launchConfigName;
        }
        labels.put(ServiceConstants.LABEL_STACK_NAME, envName);
        labels.put(ServiceConstants.LABEL_STACK_SERVICE_NAME, envName + "/" + serviceName);

        /*
         * Put label 'io.rancher.deployment.unit=this.uuid' on each one. This way
         * we can reference a set of containers later.
         */
        labels.put(ServiceConstants.LABEL_SERVICE_DEPLOYMENT_UNIT, unit.getUuid());

        /*
         *
         * Put label with launch config name
         */
        labels.put(ServiceConstants.LABEL_SERVICE_LAUNCH_CONFIG, launchConfigName);


        getLabels(fields).forEach((k, labelValue) -> {
            if (!k.startsWith(ContainerLabelAffinityConstraint.LABEL_HEADER_AFFINITY_CONTAINER_LABEL) ||
                    StringUtils.isBlank(labelValue) ||
                    labelValue.contains("/")) {
                return;
            }

            labels.put(k, envName + "/" + labelValue);
        });

        return labels;
    }

    private static void removeNamedVolumesFromLaunchConfig(Map<String, Object> deploymentUnitParams,
            Map<String, Object> deploymentData) {
        // remove named volumes from the list
        if (deploymentData.get(InstanceConstants.FIELD_DATA_VOLUMES) != null) {
            List<String> dataVolumes = new ArrayList<>();
            dataVolumes.addAll((List<String>) deploymentData.get(InstanceConstants.FIELD_DATA_VOLUMES));
            if (deploymentUnitParams.get(ServiceConstants.FIELD_INTERNAL_VOLUMES) != null) {
                for (String namedVolume : (List<String>) deploymentUnitParams
                        .get(ServiceConstants.FIELD_INTERNAL_VOLUMES)) {
                    dataVolumes.remove(namedVolume);
                }
                deploymentData.put(InstanceConstants.FIELD_DATA_VOLUMES, dataVolumes);
            }
            deploymentData.remove(ServiceConstants.FIELD_INTERNAL_VOLUMES);
        }
    }

}
