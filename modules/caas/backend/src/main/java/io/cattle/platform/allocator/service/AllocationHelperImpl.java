package io.cattle.platform.allocator.service;

import io.cattle.platform.allocator.constraint.AffinityConstraintDefinition;
import io.cattle.platform.allocator.constraint.AffinityConstraintDefinition.AffinityOps;
import io.cattle.platform.allocator.constraint.Constraint;
import io.cattle.platform.allocator.constraint.ContainerAffinityConstraint;
import io.cattle.platform.allocator.constraint.ContainerLabelAffinityConstraint;
import io.cattle.platform.allocator.constraint.HostAffinityConstraint;
import io.cattle.platform.allocator.lock.AllocateConstraintLock;
import io.cattle.platform.core.addon.metadata.HostInfo;
import io.cattle.platform.core.cache.QueryOptions;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.metadata.Metadata;
import io.cattle.platform.metadata.MetadataManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import org.jooq.tools.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.util.stream.Collectors.*;

public class AllocationHelperImpl implements AllocationHelper {

    private static final String SERVICE_NAME_MACRO = "${service_name}";
    private static final String STACK_NAME_MACRO = "${stack_name}";

    InstanceDao instanceDao;
    ObjectManager objectManager;
    MetadataManager metadataManager;

    public AllocationHelperImpl(InstanceDao instanceDao, ObjectManager objectManager, MetadataManager metadataManager) {
        this.instanceDao = instanceDao;
        this.objectManager = objectManager;
        this.metadataManager = metadataManager;
    }

    @Override
    public List<Long> getAllHostsSatisfyingHostAffinity(long clusterId, Map<String, ?> labelConstraints) {
        return getHostsSatisfyingHostAffinityInternal(true, clusterId, labelConstraints);
    }

    @Override
    public List<Long> getHostsSatisfyingHostAffinity(long clusterId, Map<String, ?> labelConstraints) {
        return getHostsSatisfyingHostAffinityInternal(false, clusterId, labelConstraints);
    }

    protected List<Long> getHostsSatisfyingHostAffinityInternal(boolean includeInvalid, long clusterId, Map<String, ?> labelConstraints) {
        List<HostInfo> hosts = includeInvalid ? getHosts(clusterId) : getActiveHosts(clusterId);

        List<Constraint> hostAffinityConstraints = getHostAffinityConstraintsFromLabels(labelConstraints);

        List<Long> acceptableHostIds = new ArrayList<>();
        for (HostInfo host : hosts) {
            if (host.getAgentId() != null && hostSatisfiesHostAffinity(clusterId, host, hostAffinityConstraints)) {
                acceptableHostIds.add(host.getId());
            }
        }
        return acceptableHostIds;
    }

    private List<Constraint> getHostAffinityConstraintsFromLabels(Map<String, ?> labelConstraints) {
        List<Constraint> constraints = extractConstraintsFromLabels(labelConstraints, null);

        List<Constraint> hostConstraints = new ArrayList<>();
        for (Constraint constraint : constraints) {
            if (constraint instanceof HostAffinityConstraint) {
                hostConstraints.add(constraint);
            }
        }
        return hostConstraints;
    }

    private boolean hostSatisfiesHostAffinity(long clusterId, HostInfo host, List<Constraint> hostAffinityConstraints) {
        for (Constraint constraint: hostAffinityConstraints) {
            AllocationCandidate candidate = new AllocationCandidate(clusterId, host.getId(), host.getUuid());
            if (!constraint.matches(candidate)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<Constraint> extractConstraintsFromEnv(Map<String, ?> env) {
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
                        constraints.add(new HostAffinityConstraint(def, this));
                    }

                } else if (affinityDef.startsWith(ContainerLabelAffinityConstraint.ENV_HEADER_AFFINITY_CONTAINER_LABEL)) {
                    affinityDef = affinityDef.substring(ContainerLabelAffinityConstraint.ENV_HEADER_AFFINITY_CONTAINER_LABEL.length());
                    AffinityConstraintDefinition def = extractAffinitionConstraintDefinitionFromEnv(affinityDef);
                    if (def != null && !StringUtils.isEmpty(def.getKey())) {
                        constraints.add(new ContainerLabelAffinityConstraint(def, this));
                    }
                }
            }
        }
        return constraints;
    }

    @Override
    public List<Constraint> extractConstraintsFromLabels(Map<String, ?> labels, Instance instance) {
        List<Constraint> constraints = new ArrayList<>();
        if (labels == null) {
            return constraints;
        }

        for (Map.Entry<String, ?> affinityDef : labels.entrySet()) {
            String key = affinityDef.getKey().toLowerCase();
            String valueStr = Objects.toString(affinityDef.getValue(), null);
            valueStr = valueStr == null ? "" : valueStr.toLowerCase();

            if (instance != null) {
                // TODO: Possibly memoize the macros so we don't need to redo the queries for Service and Environment
                valueStr = evaluateMacros(valueStr, instance);
            }

            String opStr;
            if (key.startsWith(ContainerLabelAffinityConstraint.LABEL_HEADER_AFFINITY_CONTAINER_LABEL)) {
                opStr = key.substring(ContainerLabelAffinityConstraint.LABEL_HEADER_AFFINITY_CONTAINER_LABEL.length());
                List<AffinityConstraintDefinition> defs = extractAffinityConstraintDefinitionFromLabel(opStr, valueStr, true);
                for (AffinityConstraintDefinition def: defs) {
                    constraints.add(new ContainerLabelAffinityConstraint(def, this));
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
                    constraints.add(new HostAffinityConstraint(def, this));
                }
            }
        }

        return constraints;
    }

    /*
     * Supported macros
     * ${service_name}
     * ${stack_name}
     *
     */
    private String evaluateMacros(String valueStr, Instance instance) {
        if (valueStr.contains(SERVICE_NAME_MACRO) ||
                valueStr.contains(STACK_NAME_MACRO)) {

            Map<String, String> labels = DataAccessor.getLabels(instance);
            String serviceLaunchConfigName = "";
            String stackName = "";
            if (labels != null && !labels.isEmpty()) {
                for (Map.Entry<String, String> label : labels.entrySet()) {
                    String value = label.getValue();
                    if (SystemLabels.LABEL_STACK_NAME.equals(label.getKey())) {
                        stackName = value;
                    } else if (SystemLabels.LABEL_STACK_SERVICE_NAME.equals(label.getKey())) {
                        if (value != null) {
                            int i = value.indexOf('/');
                            if (i != -1) {
                                serviceLaunchConfigName = value.substring(i + 1);
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
    public List<LockDefinition> extractAllocationLockDefinitions(Instance instance, List<Instance> instances) {
        Map<String, ?> env = DataAccessor.fieldMapRO(instance, InstanceConstants.FIELD_ENVIRONMENT);
        List<LockDefinition> lockDefs = extractAllocationLockDefinitionsFromEnv(env);

        Map<String, String> labels = DataAccessor.getLabels(instance);
        if (labels == null) {
            return lockDefs;
        }

        Map<String, String> newLabels = new HashMap<>(labels);

        // we need to merge all the affinity labels from primary containers and sickkicks
        for (Instance inst: instances) {
            Map<String, String> lbs = DataAccessor.getLabels(inst);
            for (Map.Entry<String, String> affinityDef : lbs.entrySet()) {
                String key = affinityDef.getKey().toLowerCase();
                String valueStr = affinityDef.getValue();
                if (key.startsWith(ContainerLabelAffinityConstraint.LABEL_HEADER_AFFINITY_CONTAINER_LABEL) ||
                        key.startsWith(ContainerAffinityConstraint.LABEL_HEADER_AFFINITY_CONTAINER)) {
                        newLabels.put(key, valueStr);
                }
            }
        }

        for (Map.Entry<String, String> affinityDef : newLabels.entrySet()) {
            String key = affinityDef.getKey().toLowerCase();
            String valueStr = affinityDef.getValue();
            valueStr = valueStr == null ? "" : valueStr.toLowerCase();

            if (instance != null) {
                valueStr = evaluateMacros(valueStr, instance);
            }

            String opStr;
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

    private List<LockDefinition> extractAllocationLockDefinitionsFromEnv(Map<String, ?> env) {
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

    @Override
    public boolean hostHasContainerLabel(long clusterId, String hostUuid, String labelKey, String labelValue) {
        HostInfo host = metadataManager.getMetadataForCluster(clusterId).getHost(hostUuid);
        if (host == null) {
            return false;
        }

        return Objects.equals(labelValue, host.getLabels().get(labelKey));
    }

    private List<HostInfo> getActiveHosts(long clusterId) {
        return metadataManager.getMetadataForCluster(clusterId).getHosts().stream()
                .filter((host) -> CommonStatesConstants.ACTIVE.equals(host.getState()) && CommonStatesConstants.ACTIVE.equals(host.getAgentState()))
                .collect(toList());
    }

    private List<HostInfo> getHosts(long clusterId) {
        return new ArrayList<>(metadataManager.getMetadataForCluster(clusterId).getHosts());
    }

    @Override
    public Map<String, String> getLabelsForHost(long clusterId, String hostUuid) {
        if (hostUuid == null) {
            return Collections.emptyMap();
        }
        HostInfo hostInfo = metadataManager.getMetadataForCluster(clusterId).getHost(hostUuid);
        return hostInfo == null ? Collections.emptyMap() : hostInfo.getLabels();
    }

    @Override
    public Iterator<HostInfo> iterateHosts(QueryOptions options, List<String> orderedHostUUIDs) {
        return new Iterator<HostInfo>() {
            HostInfo next;
            int orderedIndex;
            int restIndex;
            List<HostInfo> rest;
            Metadata metadata = metadataManager.getMetadataForCluster(options.getClusterId());
            Set<String> ignore = new HashSet<>();

            {
                advance();
            }

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public HostInfo next() {
                HostInfo result = next;
                advance();
                if (result != null) {
                    ignore.add(result.getUuid());
                }
                return result;
            }

            private void advance() {
                if (options.getRequestedHostId() != null) {
                    if (next != null) {
                        next = null;
                        return;
                    }

                    next = metadata.getHosts().stream()
                            .filter((host) -> host.getId() == options.getRequestedHostId())
                            .findFirst().orElse(null);
                    return;
                }

                if (orderedHostUUIDs != null) {
                    for (; orderedIndex < orderedHostUUIDs.size(); orderedIndex++) {
                        next = metadata.getHost(orderedHostUUIDs.get(orderedIndex));
                        if (next != null && !ignore.contains(next.getUuid()) && validHost(next)) {
                            orderedIndex++;
                            return;
                        }
                    }
                }

                if (rest == null) {
                    rest = new ArrayList<>(metadata.getHosts());
                    Collections.shuffle(rest);
                }

                for (; restIndex < rest.size() ; restIndex++) {
                    next = rest.get(restIndex);
                    if (!ignore.contains(next.getUuid()) && validHost(next)) {
                        restIndex++;
                        return;
                    }
                }

                next = null;
            }
        };
    }

    private boolean validHost(HostInfo hostInfo) {
        if (hostInfo == null) {
            return false;
        }
        return CommonStatesConstants.ACTIVE.equals(hostInfo.getAgentState()) &&
                CommonStatesConstants.ACTIVE.equals(hostInfo.getState());
    }

}
