package io.cattle.platform.inator.util;

import io.cattle.platform.allocator.constraint.ContainerLabelAffinityConstraint;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.inator.wrapper.DeploymentUnitWrapper;
import io.cattle.platform.inator.wrapper.RevisionWrapper;
import io.cattle.platform.inator.wrapper.StackWrapper;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class InstanceFactory {

    public static Map<String, Object> createInstanceData(Map<String, Object> launchConfig,
            StackWrapper stack,
            RevisionWrapper service,
            DeploymentUnitWrapper unit,
            String launchConfigName,
            String instanceName) {

        Map<String, Object> fields = new HashMap<>(launchConfig);
        addAdditionalFields(fields, stack, service, unit, launchConfigName);
        addDns(fields, stack, service);
        addHostnameOverride(instanceName, fields);

        fields.put(ObjectMetaDataManager.NAME_FIELD, instanceName);
        fields.put(ServiceConstants.FIELD_SYSTEM, ServiceConstants.isSystem(stack.getInternal()));

        return fields;
    }


    private static void addHostnameOverride(String instanceName, Map<String, Object> fields) {
        Map<String, String> labels = getLabels(fields);
        if (!"container_name".equalsIgnoreCase(labels.get(ServiceConstants.LABEL_OVERRIDE_HOSTNAME))) {
            return;
        }

        Object domainName = fields.get(InstanceConstants.FIELD_DOMAIN_NAME);
        String overrideName = getOverrideHostName(domainName, instanceName);
        fields.put(InstanceConstants.FIELD_HOSTNAME, overrideName);
    }

    protected static Map<String, String> getLabels(Map<String, Object> fields) {
        return CollectionUtils.toMap(fields.get(InstanceConstants.FIELD_LABELS));
    }

    protected static void setLabels(Map<String, Object> fields, Map<String, String> labels) {
        fields.put(InstanceConstants.FIELD_LABELS, labels);
    }

    protected static void addDns(Map<String, Object> fields, StackWrapper stack, RevisionWrapper service) {
        Map<String, String> labels = CollectionUtils.toMap(fields.get(InstanceConstants.FIELD_LABELS));
        if ("false".equalsIgnoreCase(labels.get(SystemLabels.LABEL_USE_RANCHER_DNS))) {
            return;
        }
        List<Object> dns = new ArrayList<>(CollectionUtils.toList(fields.get(InstanceConstants.FIELD_DNS_SEARCH)));
        for (String entry : getSearchDomains(stack, service)) {
            if (!dns.contains(entry)) {
                dns.add(entry);
            }
        }
        fields.put(InstanceConstants.FIELD_DNS_SEARCH, dns);
    }

    protected static List<String> getSearchDomains(StackWrapper stack, RevisionWrapper service) {
        String stackNamespace = ServiceUtil.getStackNamespace(stack.getName());
        String serviceNamespace = ServiceUtil.getServiceNamespace(stack.getName(), service.getName());
        return Arrays.asList(stackNamespace, serviceNamespace);
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

    protected static void addAdditionalFields(Map<String, Object> fields, StackWrapper stack, RevisionWrapper revision,
            DeploymentUnitWrapper unit, String launchConfigName) {
        fields.put(InstanceConstants.FIELD_DEPLOYMENT_UNIT_UUID, unit.getUuid());
        fields.put(InstanceConstants.FIELD_DEPLOYMENT_UNIT_ID, unit.getId());
        fields.put(InstanceConstants.FIELD_STACK_ID, stack.getId());
        fields.put(InstanceConstants.FIELD_SERVICE_INDEX, unit.getServiceIndex());
        fields.put(ObjectMetaDataManager.ACCOUNT_FIELD, stack.getAccountId());
        fields.put(InstanceConstants.FIELD_REQUESTED_HOST_ID, unit.getHostId());
        if (!fields.containsKey(ObjectMetaDataManager.KIND_FIELD)) {
            fields.put(ObjectMetaDataManager.KIND_FIELD, InstanceConstants.KIND_CONTAINER);
        }

        if (revision != null) {
            addServiceFields(fields, stack, revision, unit, launchConfigName);
        }
    }

    protected static void addServiceFields(Map<String, Object> fields, StackWrapper stack, RevisionWrapper revision, DeploymentUnitWrapper unit,
            String launchConfigName) {
        if (revision.getServiceId() != null) {
            Map<String, String> labels = createServiceLabels(fields, revision, stack, unit, launchConfigName);
            addLabels(fields, labels);
        }
        fields.put(InstanceConstants.FIELD_LAUNCH_CONFIG_NAME, launchConfigName);
        fields.put(InstanceConstants.FIELD_SERVICE_ID, revision.getServiceId());
        fields.put(InstanceConstants.FIELD_REVISION_ID, revision.getId());
    }

    protected static void addLabels(Map<String, Object> fields, Map<String, String> labels) {
        Map<String, String> toLabels = getLabels(fields);
        toLabels.putAll(labels);
        setLabels(fields, toLabels);
    }

    protected static Map<String, String> createServiceLabels(Map<String, Object> fields, RevisionWrapper service, StackWrapper stack,
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


        /*
         * Translate affinity to io.rancher.stack_service.name=${service} to io.rancher.stack_service.name=${env}/${service}
         */
        getLabels(fields).forEach((k, labelValue) -> {
            if (!k.startsWith(ContainerLabelAffinityConstraint.LABEL_HEADER_AFFINITY_CONTAINER_LABEL) ||
                    StringUtils.isBlank(labelValue) ||
                    labelValue.contains("/") ||
                    !labelValue.startsWith(ServiceConstants.LABEL_STACK_SERVICE_NAME + "=")) {
                return;
            }

            String[] parts = labelValue.split("=", 2);
            if (parts.length == 2) {
                labels.put(k, String.format("%s=%s/%s", parts[0], envName, parts[1]));
            }
        });

        return labels;
    }

}
