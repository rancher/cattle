package io.cattle.platform.allocator.service;

import static io.cattle.platform.core.model.tables.HostTable.HOST;
import io.cattle.platform.allocator.constraint.AffinityConstraintDefinition;
import io.cattle.platform.allocator.constraint.Constraint;
import io.cattle.platform.allocator.constraint.ContainerAffinityConstraint;
import io.cattle.platform.allocator.constraint.ContainerLabelAffinityConstraint;
import io.cattle.platform.allocator.constraint.HostAffinityConstraint;
import io.cattle.platform.allocator.constraint.AffinityConstraintDefinition.AffinityOps;
import io.cattle.platform.allocator.dao.AllocatorDao;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.object.ObjectManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.jooq.tools.StringUtils;

public class AllocatorServiceImpl implements AllocatorService {

    @Inject
    AllocatorDao allocatorDao;

    @Inject
    InstanceDao instanceDao;

    @Inject
    ObjectManager objectManager;

    @Override
    public List<Long> getHostsForGlobalService(Long accountId, Map<String, String> labels) {
        List<Long> acceptableHostIds = new ArrayList<Long>();

        List<Host> hosts = objectManager.find(Host.class, HOST.ACCOUNT_ID, accountId, HOST.STATE, CommonStatesConstants.ACTIVE, HOST.REMOVED, null);
        List<Constraint> constraints = extractConstraintsFromLabels(labels);

        List<Constraint> hostConstraints = new ArrayList<Constraint>();
        for (Constraint constraint : constraints) {
            if (constraint instanceof HostAffinityConstraint) {
                hostConstraints.add(constraint);
            }
        }

        for (Host host : hosts) {
            boolean acceptable = true;
            for (Constraint constraint: hostConstraints) {
                AllocationCandidate candidate = new AllocationCandidate();
                Set<Long> hostIds = new HashSet<Long>();
                hostIds.add(host.getId());
                candidate.setHosts(hostIds);
                if (!constraint.matches(null, candidate)) {
                    acceptable = false;
                    break;
                }
            }
            if (acceptable) {
                acceptableHostIds.add(host.getId());
            }
        }
        return acceptableHostIds;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<Constraint> extractConstraintsFromEnv(Map env) {
        List<Constraint> constraints = new ArrayList<Constraint>();
        if (env != null) {
            Set<String> affinityDefinitions = env.keySet();
            for (String affinityDef : affinityDefinitions) {
                if (affinityDef == null) {
                    continue;
                }

                if (affinityDef.startsWith(ContainerLabelAffinityConstraint.ENV_HEADER_AFFINITY_CONTAINER_LABEL)) {
                    affinityDef = affinityDef.substring(ContainerLabelAffinityConstraint.ENV_HEADER_AFFINITY_CONTAINER_LABEL.length());
                    AffinityConstraintDefinition def = extractAffinitionConstraintDefinitionFromEnv(affinityDef);
                    if (def != null && !StringUtils.isEmpty(def.getKey())) {
                        constraints.add(new ContainerLabelAffinityConstraint(def, allocatorDao));
                    }

                } else if (affinityDef.startsWith(ContainerAffinityConstraint.ENV_HEADER_AFFINITY_CONTAINER)) {
                    affinityDef = affinityDef.substring(ContainerAffinityConstraint.ENV_HEADER_AFFINITY_CONTAINER.length());
                    AffinityConstraintDefinition def = extractAffinitionConstraintDefinitionFromEnv(affinityDef);
                    if (def != null && !StringUtils.isEmpty(def.getValue())) {
                        constraints.add(new ContainerAffinityConstraint(def, objectManager, instanceDao));
                    }

                } else if (affinityDef.startsWith(HostAffinityConstraint.ENV_HEADER_AFFINITY_HOST_LABEL)) {
                    affinityDef = affinityDef.substring(HostAffinityConstraint.ENV_HEADER_AFFINITY_HOST_LABEL.length());
                    AffinityConstraintDefinition def = extractAffinitionConstraintDefinitionFromEnv(affinityDef);
                    if (def != null && !StringUtils.isEmpty(def.getKey())) {
                        constraints.add(new HostAffinityConstraint(def, allocatorDao));
                    }

                }
            }
        }
        return constraints;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<Constraint> extractConstraintsFromLabels(Map labels) {
        List<Constraint> constraints = new ArrayList<Constraint>();
        if (labels != null) {
            Set<String> affinityDefinitions = labels.keySet();
            for (String affinityDef : affinityDefinitions) {
                if (affinityDef == null) {
                    continue;
                }

                if (affinityDef.startsWith(ContainerLabelAffinityConstraint.LABEL_HEADER_AFFINITY_CONTAINER_LABEL)) {
                    affinityDef = affinityDef.substring(ContainerLabelAffinityConstraint.LABEL_HEADER_AFFINITY_CONTAINER_LABEL.length());
                    AffinityConstraintDefinition def = extractAffinitionConstraintDefinitionFromLabel(affinityDef);
                    if (def != null && !StringUtils.isEmpty(def.getKey())) {
                        constraints.add(new ContainerLabelAffinityConstraint(def, allocatorDao));
                    }

                } else if (affinityDef.startsWith(ContainerAffinityConstraint.LABEL_HEADER_AFFINITY_CONTAINER)) {
                    affinityDef = affinityDef.substring(ContainerAffinityConstraint.LABEL_HEADER_AFFINITY_CONTAINER.length());
                    AffinityConstraintDefinition def = extractAffinitionConstraintDefinitionFromLabel(affinityDef);
                    if (def != null && !StringUtils.isEmpty(def.getValue())) {
                        constraints.add(new ContainerAffinityConstraint(def, objectManager, instanceDao));
                    }

                } else if (affinityDef.startsWith(HostAffinityConstraint.LABEL_HEADER_AFFINITY_HOST_LABEL)) {
                    affinityDef = affinityDef.substring(HostAffinityConstraint.LABEL_HEADER_AFFINITY_HOST_LABEL.length());
                    AffinityConstraintDefinition def = extractAffinitionConstraintDefinitionFromLabel(affinityDef);
                    if (def != null && !StringUtils.isEmpty(def.getKey())) {
                        constraints.add(new HostAffinityConstraint(def, allocatorDao));
                    }

                }
            }
        }
        return constraints;
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

    private AffinityConstraintDefinition extractAffinitionConstraintDefinitionFromLabel(String definitionString) {
        for (AffinityOps op : AffinityOps.values()) {
            int i = definitionString.indexOf(op.getLabelSymbol());
            if (i != -1) {
                String key = definitionString.substring(0, i);
                String value = definitionString.substring(i + op.getLabelSymbol().length());
                return new AffinityConstraintDefinition(op, key, value);
            }
        }
        return null;
    }
}
