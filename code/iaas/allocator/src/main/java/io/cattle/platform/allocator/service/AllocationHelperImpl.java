package io.cattle.platform.allocator.service;

import static io.cattle.platform.core.model.tables.ServiceTable.*;

import io.cattle.platform.allocator.constraint.AffinityConstraintDefinition;
import io.cattle.platform.allocator.constraint.AffinityConstraintDefinition.AffinityOps;
import io.cattle.platform.allocator.constraint.Constraint;
import io.cattle.platform.allocator.constraint.ContainerAffinityConstraint;
import io.cattle.platform.allocator.constraint.ContainerLabelAffinityConstraint;
import io.cattle.platform.allocator.constraint.HostAffinityConstraint;
import io.cattle.platform.allocator.dao.AllocatorDao;
import io.cattle.platform.allocator.lock.AllocateConstraintLock;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.dao.LabelsDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.resource.service.EnvironmentResourceManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.jooq.tools.StringUtils;

public class AllocationHelperImpl implements AllocationHelper {

    private static final String SERVICE_NAME_MACRO = "${service_name}";
    private static final String STACK_NAME_MACRO = "${stack_name}";

    @Inject
    LabelsDao labelsDao;

    @Inject
    AllocatorDao allocatorDao;

    @Inject
    InstanceDao instanceDao;

    @Inject
    ObjectManager objectManager;

    @Inject
    JsonMapper jsonMapper;

    @Inject
    AllocatorService allocatorService;

    @Inject
    EnvironmentResourceManager envResourceManager;

    @Override
    public List<Long> getAllHostsSatisfyingHostAffinity(Long accountId, Map<String, String> labelConstraints) {
        return getHostsSatisfyingHostAffinityInternal(true, accountId, labelConstraints);
    }

    @Override
    public List<Long> getHostsSatisfyingHostAffinity(Long accountId, Map<String, String> labelConstraints) {
        return getHostsSatisfyingHostAffinityInternal(false, accountId, labelConstraints);
    }

    protected List<Long> getHostsSatisfyingHostAffinityInternal(boolean includeRemoved, Long accountId, Map<String, String> labelConstraints) {
        List<? extends Long> hosts = includeRemoved ? envResourceManager.getHosts(accountId) : envResourceManager.getActiveHosts(accountId);

        List<Constraint> hostAffinityConstraints = getHostAffinityConstraintsFromLabels(labelConstraints);

        List<Long> acceptableHostIds = new ArrayList<>();
        for (Long hostId : hosts) {
            if (hostSatisfiesHostAffinity(hostId, hostAffinityConstraints)) {
                acceptableHostIds.add(hostId);
            }
        }
        return acceptableHostIds;
    }

    private List<Constraint> getHostAffinityConstraintsFromLabels(Map<String, String> labelConstraints) {
        List<Constraint> constraints = extractConstraintsFromLabels(labelConstraints, null);

        List<Constraint> hostConstraints = new ArrayList<>();
        for (Constraint constraint : constraints) {
            if (constraint instanceof HostAffinityConstraint) {
                hostConstraints.add(constraint);
            }
        }
        return hostConstraints;
    }

    private boolean hostSatisfiesHostAffinity(long hostId, List<Constraint> hostAffinityConstraints) {
        for (Constraint constraint: hostAffinityConstraints) {
            AllocationCandidate candidate = new AllocationCandidate();
            candidate.setHost(hostId);
            if (!constraint.matches(candidate)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void normalizeLabels(long stackId, Map<String, String> systemLabels, Map<String, String> serviceUserLabels) {
        String stackName = systemLabels.get(SystemLabels.LABEL_STACK_NAME);
        String stackServiceNameWithLaunchConfig = systemLabels.get(SystemLabels.LABEL_STACK_SERVICE_NAME);
        String launchConfig = systemLabels.get(SystemLabels.LABEL_SERVICE_LAUNCH_CONFIG);

        Set<String> serviceNamesInStack = getServiceNamesInStack(stackId);

        for (Map.Entry<String, String> entry : serviceUserLabels.entrySet()) {
            String labelValue = entry.getValue();
            if (entry.getKey().startsWith(ContainerLabelAffinityConstraint.LABEL_HEADER_AFFINITY_CONTAINER_LABEL) &&
                    labelValue != null) {
                String userEnteredServiceName = null;
                if (labelValue.startsWith(SystemLabels.LABEL_STACK_SERVICE_NAME)) {
                    userEnteredServiceName = labelValue.substring(SystemLabels.LABEL_STACK_SERVICE_NAME.length() + 1);
                }
                if (userEnteredServiceName != null) {
                    String[] components = userEnteredServiceName.split("/");
                    if (components.length == 1 &&
                            stackServiceNameWithLaunchConfig != null) {
                        if (serviceNamesInStack.contains(userEnteredServiceName.toLowerCase())) {
                            // prepend stack name
                            userEnteredServiceName = stackName + "/" + userEnteredServiceName;
                        }
                    }
                    if (!SystemLabels.PRIMARY_LAUNCH_CONFIG_NAME.equals(launchConfig) &&
                            stackServiceNameWithLaunchConfig.startsWith(userEnteredServiceName)) {
                        // automatically append secondary launchConfig
                        userEnteredServiceName = userEnteredServiceName + "/" + launchConfig;
                    }
                    entry.setValue(SystemLabels.LABEL_STACK_SERVICE_NAME + "=" + userEnteredServiceName);
                }
            }
        }
    }

    // TODO: Fix repeated DB call even if DB's cache no longer hits the disk
    private Set<String> getServiceNamesInStack(long stackId) {
        Set<String> servicesInEnv = new HashSet<>();

        List<? extends Service> services = objectManager.find(Service.class, SERVICE.STACK_ID, stackId, SERVICE.REMOVED,
                null);
        for (Service service : services) {
            servicesInEnv.add(service.getName().toLowerCase());
        }
        return servicesInEnv;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<Constraint> extractConstraintsFromEnv(Map env) {
        List<Constraint> constraints = new ArrayList<>();
        if (env != null) {
            Set<String> affinityDefinitions = env.keySet();
            for (String affinityDef : affinityDefinitions) {
                if (affinityDef == null) {
                    continue;
                }

                if (affinityDef.startsWith(ContainerAffinityConstraint.ENV_HEADER_AFFINITY_CONTAINER)) {
                    affinityDef = affinityDef.substring(ContainerAffinityConstraint.ENV_HEADER_AFFINITY_CONTAINER.length());
                    AffinityConstraintDefinition def = extractAffinitionConstraintDefinitionFromEnv(affinityDef);
                    if (def != null && !StringUtils.isEmpty(def.getValue())) {
                        constraints.add(new ContainerAffinityConstraint(def, objectManager, instanceDao));
                    }

                } else if (affinityDef.startsWith(HostAffinityConstraint.ENV_HEADER_AFFINITY_HOST_LABEL)) {
                    affinityDef = affinityDef.substring(HostAffinityConstraint.ENV_HEADER_AFFINITY_HOST_LABEL.length());
                    AffinityConstraintDefinition def = extractAffinitionConstraintDefinitionFromEnv(affinityDef);
                    if (def != null && !StringUtils.isEmpty(def.getKey())) {
                        constraints.add(new HostAffinityConstraint(def, envResourceManager));
                    }

                } else if (affinityDef.startsWith(ContainerLabelAffinityConstraint.ENV_HEADER_AFFINITY_CONTAINER_LABEL)) {
                    affinityDef = affinityDef.substring(ContainerLabelAffinityConstraint.ENV_HEADER_AFFINITY_CONTAINER_LABEL.length());
                    AffinityConstraintDefinition def = extractAffinitionConstraintDefinitionFromEnv(affinityDef);
                    if (def != null && !StringUtils.isEmpty(def.getKey())) {
                        constraints.add(new ContainerLabelAffinityConstraint(def, envResourceManager));
                    }
                }
            }
        }
        return constraints;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<Constraint> extractConstraintsFromLabels(Map labels, Instance instance) {
        List<Constraint> constraints = new ArrayList<>();
        if (labels == null) {
            return constraints;
        }

        Iterator<Map.Entry> iter = labels.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry affinityDef = iter.next();
            String key = ((String)affinityDef.getKey()).toLowerCase();
            String valueStr = (String)affinityDef.getValue();
            valueStr = valueStr == null ? "" : valueStr.toLowerCase();

            if (instance != null) {
                // TODO: Possibly memoize the macros so we don't need to redo the queries for Service and Environment
                valueStr = evaluateMacros(valueStr, instance);
            }

            String opStr = "";
            if (key.startsWith(ContainerLabelAffinityConstraint.LABEL_HEADER_AFFINITY_CONTAINER_LABEL)) {
                opStr = key.substring(ContainerLabelAffinityConstraint.LABEL_HEADER_AFFINITY_CONTAINER_LABEL.length());
                List<AffinityConstraintDefinition> defs = extractAffinityConstraintDefinitionFromLabel(opStr, valueStr, true);
                for (AffinityConstraintDefinition def: defs) {
                    constraints.add(new ContainerLabelAffinityConstraint(def, envResourceManager));
                }

            } else if (key.startsWith(ContainerAffinityConstraint.LABEL_HEADER_AFFINITY_CONTAINER)) {
                opStr = key.substring(ContainerAffinityConstraint.LABEL_HEADER_AFFINITY_CONTAINER.length());
                List<AffinityConstraintDefinition> defs = extractAffinityConstraintDefinitionFromLabel(opStr, valueStr, false);
                for (AffinityConstraintDefinition def: defs) {
                    constraints.add(new ContainerAffinityConstraint(def, objectManager, instanceDao));
                }

            } else if (key.startsWith(HostAffinityConstraint.LABEL_HEADER_AFFINITY_HOST_LABEL)) {
                opStr = key.substring(HostAffinityConstraint.LABEL_HEADER_AFFINITY_HOST_LABEL.length());
                List<AffinityConstraintDefinition> defs = extractAffinityConstraintDefinitionFromLabel(opStr, valueStr, true);
                for (AffinityConstraintDefinition def: defs) {
                    constraints.add(new HostAffinityConstraint(def, envResourceManager));
                }
            }
        }

        return constraints;
    }

    /**
     * Supported macros
     * ${service_name}
     * ${stack_name}
     * LEGACY:
     *
     * @param valueStr
     * @param instance
     * @return
     */
    @SuppressWarnings("unchecked")
    private String evaluateMacros(String valueStr, Instance instance) {
        if (valueStr.indexOf(SERVICE_NAME_MACRO) != -1 ||
                valueStr.indexOf(STACK_NAME_MACRO) != -1) {

            Map<String, String> labels = DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_LABELS).as(Map.class);
            String serviceLaunchConfigName = "";
            String stackName = "";
            if (labels != null && !labels.isEmpty()) {
                for (Map.Entry<String, String> label : labels.entrySet()) {
                    if (SystemLabels.LABEL_STACK_NAME.equals(label.getKey())) {
                        stackName = label.getValue();
                    } else if (SystemLabels.LABEL_STACK_SERVICE_NAME.equals(label.getKey())) {
                        if (label.getValue() != null) {
                            int i = label.getValue().indexOf('/');
                            if (i != -1) {
                                serviceLaunchConfigName = label.getValue().substring(i + 1);
                            }
                        }
                    }
                }
            }
            if (!StringUtils.isBlank(stackName)) {
                valueStr = valueStr.replace(STACK_NAME_MACRO, stackName);
            }

            if (!StringUtils.isBlank(serviceLaunchConfigName)) {
                valueStr = valueStr.replace(SERVICE_NAME_MACRO, serviceLaunchConfigName);
            }
        }

        return valueStr;
    }

    private AffinityConstraintDefinition extractAffinitionConstraintDefinitionFromEnv(String definitionString) {
        for (AffinityOps op : AffinityOps.values()) {
            int i = definitionString.indexOf(op.getEnvSymbol());
            if (i != -1) {
                String key = definitionString.substring(0, i);
                String value = definitionString.substring(i + op.getEnvSymbol().length());
                return new AffinityConstraintDefinition(op, key, value);
            }
        }
        return null;
    }

    private List<AffinityConstraintDefinition> extractAffinityConstraintDefinitionFromLabel(String opStr, String valueStr, boolean keyValuePairs) {
        List<AffinityConstraintDefinition> defs = new ArrayList<>();

        AffinityOps affinityOp = null;
        for (AffinityOps op : AffinityOps.values()) {
            if (op.getLabelSymbol().equals(opStr)) {
                affinityOp = op;
                break;
            }
        }
        if (affinityOp == null) {
            return defs;
        }

        if (StringUtils.isEmpty(valueStr)) {
            return defs;
        }

        String[] values = valueStr.split(",");
        for (String value : values) {
            if (StringUtils.isEmpty(value)) {
                continue;
            }
            if (keyValuePairs && value.indexOf('=') != -1) {
                String[] pair = value.split("=");
                defs.add(new AffinityConstraintDefinition(affinityOp, pair[0], pair[1]));
            } else {
                defs.add(new AffinityConstraintDefinition(affinityOp, null, value));
            }
        }
        return defs;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<LockDefinition> extractAllocationLockDefinitions(Instance instance, List<Instance> instances) {
        Map env = DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_ENVIRONMENT).as(jsonMapper, Map.class);
        List<LockDefinition> lockDefs = extractAllocationLockDefinitionsFromEnv(env);

        Map labels = DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_LABELS).as(jsonMapper, Map.class);
        if (labels == null) {
            return lockDefs;
        }
        // we need to merge all the affinity labels from primary containers and sickkicks
        for (Instance inst: instances) {
            Map lbs = DataAccessor.fields(inst).withKey(InstanceConstants.FIELD_LABELS).as(jsonMapper, Map.class);
            Iterator<Map.Entry> it = lbs.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry affinityDef = it.next();
                String key = ((String)affinityDef.getKey()).toLowerCase();
                String valueStr = (String)affinityDef.getValue();
                if (key.startsWith(ContainerLabelAffinityConstraint.LABEL_HEADER_AFFINITY_CONTAINER_LABEL) ||
                        key.startsWith(ContainerAffinityConstraint.LABEL_HEADER_AFFINITY_CONTAINER)) {
                        labels.put(key, valueStr);
                }
            }
        }

        Iterator<Map.Entry> iter = labels.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry affinityDef = iter.next();
            String key = ((String)affinityDef.getKey()).toLowerCase();
            String valueStr = (String)affinityDef.getValue();
            valueStr = valueStr == null ? "" : valueStr.toLowerCase();

            if (instance != null) {
                valueStr = evaluateMacros(valueStr, instance);
            }

            String opStr = "";
            if (key.startsWith(ContainerLabelAffinityConstraint.LABEL_HEADER_AFFINITY_CONTAINER_LABEL)) {
                opStr = key.substring(ContainerLabelAffinityConstraint.LABEL_HEADER_AFFINITY_CONTAINER_LABEL.length());
                List<AffinityConstraintDefinition> defs = extractAffinityConstraintDefinitionFromLabel(opStr, valueStr, true);
                for (AffinityConstraintDefinition def: defs) {
                    lockDefs.add(new AllocateConstraintLock(AllocateConstraintLock.Type.AFFINITY, def.getValue()));
                }

            } else if (key.startsWith(ContainerAffinityConstraint.LABEL_HEADER_AFFINITY_CONTAINER)) {
                opStr = key.substring(ContainerAffinityConstraint.LABEL_HEADER_AFFINITY_CONTAINER.length());
                List<AffinityConstraintDefinition> defs = extractAffinityConstraintDefinitionFromLabel(opStr, valueStr, false);
                for (AffinityConstraintDefinition def: defs) {
                    lockDefs.add(new AllocateConstraintLock(AllocateConstraintLock.Type.AFFINITY, def.getValue()));
                }

            }
        }
        return lockDefs;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private List<LockDefinition> extractAllocationLockDefinitionsFromEnv(Map env) {
        List<LockDefinition> constraints = new ArrayList<>();
        if (env != null) {
            Set<String> affinityDefinitions = env.keySet();
            for (String affinityDef : affinityDefinitions) {
                if (affinityDef == null) {
                    continue;
                }

                if (affinityDef.startsWith(ContainerAffinityConstraint.ENV_HEADER_AFFINITY_CONTAINER)) {
                    affinityDef = affinityDef.substring(ContainerAffinityConstraint.ENV_HEADER_AFFINITY_CONTAINER.length());
                    AffinityConstraintDefinition def = extractAffinitionConstraintDefinitionFromEnv(affinityDef);
                    if (def != null && !StringUtils.isEmpty(def.getValue())) {
                        constraints.add(new AllocateConstraintLock(AllocateConstraintLock.Type.AFFINITY, def.getValue()));
                    }

                } else if (affinityDef.startsWith(ContainerLabelAffinityConstraint.ENV_HEADER_AFFINITY_CONTAINER_LABEL)) {
                    affinityDef = affinityDef.substring(ContainerLabelAffinityConstraint.ENV_HEADER_AFFINITY_CONTAINER_LABEL.length());
                    AffinityConstraintDefinition def = extractAffinitionConstraintDefinitionFromEnv(affinityDef);
                    if (def != null && !StringUtils.isEmpty(def.getKey())) {
                        constraints.add(new AllocateConstraintLock(AllocateConstraintLock.Type.AFFINITY, def.getValue()));
                    }
                }
            }
        }
        return constraints;
    }


}
