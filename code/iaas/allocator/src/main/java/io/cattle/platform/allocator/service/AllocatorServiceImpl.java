package io.cattle.platform.allocator.service;

import io.cattle.platform.allocator.constraint.AffinityConstraintDefinition;
import io.cattle.platform.allocator.constraint.Constraint;
import io.cattle.platform.allocator.constraint.ContainerAffinityConstraint;
import io.cattle.platform.allocator.constraint.ContainerLabelAffinityConstraint;
import io.cattle.platform.allocator.constraint.HostAffinityConstraint;
import io.cattle.platform.allocator.constraint.AffinityConstraintDefinition.AffinityOps;
import io.cattle.platform.allocator.dao.AllocatorDao;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.servicediscovery.api.dao.ServiceDao;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.jooq.tools.StringUtils;

public class AllocatorServiceImpl implements AllocatorService {

    private static final String SERVICE_NAME_MACRO = "${service_name}";
    private static final String STACK_NAME_MACRO = "${stack_name}";

    @Inject
    ServiceDao serviceDao;

    @Inject
    AllocatorDao allocatorDao;

    @Inject
    InstanceDao instanceDao;

    @Inject
    ObjectManager objectManager;

    @Override
    public List<Long> getHostsSatisfyingHostAffinity(Long accountId, Map<String, String> labelConstraints) {
        List<? extends Host> hosts = allocatorDao.getActiveHosts(accountId);

        List<Constraint> hostAffinityConstraints = getHostAffinityConstraintsFromLabels(labelConstraints);

        List<Long> acceptableHostIds = new ArrayList<Long>();
        for (Host host : hosts) {
            if (hostSatisfiesHostAffinity(host.getId(), hostAffinityConstraints)) {
                acceptableHostIds.add(host.getId());
            }
        }
        return acceptableHostIds;
    }

    @Override
    public boolean hostChangesAffectsHostAffinityRules(long hostId, Map<String, String> labelConstraints) {
        List<Constraint> hostAffinityConstraints = getHostAffinityConstraintsFromLabels(labelConstraints);
        // NOTE: There is a bug since the current check does not detect the converse.
        // For example, if the host currently satisfies the hostAffinityConstraints but the
        // change causes it to no longer satisfy the condition.  This is fine for now since
        // we currently do not want to remove containers for the user.
        return hostSatisfiesHostAffinity(hostId, hostAffinityConstraints);
    }

    private List<Constraint> getHostAffinityConstraintsFromLabels(Map<String, String> labelConstraints) {
        List<Constraint> constraints = extractConstraintsFromLabels(labelConstraints, null);

        List<Constraint> hostConstraints = new ArrayList<Constraint>();
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
            Set<Long> hostIds = new HashSet<Long>();
            hostIds.add(hostId);
            candidate.setHosts(hostIds);
            if (!constraint.matches(null, candidate)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void mergeLabels(Map<String, String> srcMap, Map<String, String> destMap) {
        if (srcMap == null || destMap == null) {
            return;
        }
        for (Map.Entry<String, String> entry : srcMap.entrySet()) {
            if (entry.getKey().startsWith("io.rancher.scheduler.affinity")) {
                // merge labels
                String destValue = destMap.get(entry.getKey());
                if (StringUtils.isEmpty(destValue)) {
                    destMap.put(entry.getKey(), entry.getValue());
                } else if (StringUtils.isEmpty(entry.getValue())) {
                    continue;
                } else if (!destValue.contains(entry.getValue())) {
                    destMap.put(entry.getKey(), destValue + "," + entry.getValue());
                }
            } else {
                // overwrite label value
                destMap.put(entry.getKey(), entry.getValue());
            }
        }
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
    public List<Constraint> extractConstraintsFromLabels(Map labels, Instance instance) {
        List<Constraint> constraints = new ArrayList<Constraint>();
        if (labels == null) {
            return constraints;
        }

        Iterator<Map.Entry> iter = labels.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry affinityDef = iter.next();
            String key = (String)affinityDef.getKey();
            String valueStr = (String)affinityDef.getValue();

            if (instance != null) {
                // TODO: Possibly memoize the macros so we don't need to redo the queries for Service and Environment
                valueStr = evaluateMacros(valueStr, instance);
            }

            String opStr = "";
            if (key.startsWith(ContainerLabelAffinityConstraint.LABEL_HEADER_AFFINITY_CONTAINER_LABEL)) {
                opStr = key.substring(ContainerLabelAffinityConstraint.LABEL_HEADER_AFFINITY_CONTAINER_LABEL.length());
                List<AffinityConstraintDefinition> defs = extractAffinityConstraintDefinitionFromLabel(opStr, valueStr, true);
                for (AffinityConstraintDefinition def: defs) {
                    constraints.add(new ContainerLabelAffinityConstraint(def, allocatorDao));
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
                    constraints.add(new HostAffinityConstraint(def, allocatorDao));
                }
            }
        }

        return constraints;
    }

    /**
     * Supported macros
     * ${service_name}
     * ${project_name}
     *
     * @param valueStr
     * @param instance
     * @return
     */
    private String evaluateMacros(String valueStr, Instance instance) {
        if (valueStr.indexOf(SERVICE_NAME_MACRO) != -1 || valueStr.indexOf(STACK_NAME_MACRO) != -1) {
            Service service = null;

            List<? extends Service> services = serviceDao.findServicesFor(instance);
            if (services.size() > 0) {
                service = services.get(0);
            }

            if (service != null) {
                valueStr = valueStr.replace(SERVICE_NAME_MACRO, service.getName());
            }

            if (valueStr.indexOf(STACK_NAME_MACRO) != -1 && service != null) {
                Environment project = objectManager.loadResource(Environment.class, service.getEnvironmentId());
                if (project != null) {
                    valueStr = valueStr.replace(STACK_NAME_MACRO, project.getName());
                }
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
        List<AffinityConstraintDefinition> defs = new ArrayList<AffinityConstraintDefinition>();

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
}
