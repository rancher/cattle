package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.constraint.AffinityConstraintDefinition.AffinityOps;
import io.cattle.platform.allocator.dao.AllocatorDao;
import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationLog;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.jooq.tools.StringUtils;

/**
 * Future optimization: For hard constraints, we might be able to update the DB query to do
 * the lookup.
 *
 * @author sonchang
 *
 */
public class AffinityConstraintsProvider implements AllocationConstraintsProvider {

    @Inject
    AllocatorDao allocatorDao;

    @Inject
    InstanceDao instanceDao;

    @Inject
    ObjectManager objectManager;

    @Inject
    JsonMapper jsonMapper;

    @SuppressWarnings("rawtypes")
    @Override
    public void appendConstraints(AllocationAttempt attempt, AllocationLog log,
            List<Constraint> constraints) {
        Instance instance = attempt.getInstance();
        if (instance == null) {
            return;
        }

        Map env = DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_ENVIRONMENT).as(jsonMapper, Map.class);
        // TODO: hack for now.  assuming all affinity:constraint specs are just found in the key
        List<Constraint> affinityConstraintsFromEnv = extractConstraintsFromEnv(env);
        for (Constraint constraint : affinityConstraintsFromEnv) {
            constraints.add(constraint);
        }

        // Currently, intentionally duplicating code to be explicit
        Map labels = DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_LABELS).as(jsonMapper, Map.class);
        List<Constraint> affinityConstraintsFromLabels = extractConstraintsFromLabels(labels);
        for (Constraint constraint : affinityConstraintsFromLabels) {
            constraints.add(constraint);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private List<Constraint> extractConstraintsFromEnv(Map env) {
        List<Constraint> constraints = new ArrayList<Constraint>();
        if (env != null) {
            Set<String> affinityDefinitions = env.keySet();
            for (String affinityDef : affinityDefinitions) {
                if (affinityDef == null) {
                    continue;
                }

                if (affinityDef.startsWith(ContainerLabelAffinityConstraint.ENV_HEADER_AFFINITY_CONTAINER_LABEL)) {
                    affinityDef = affinityDef.substring(ContainerLabelAffinityConstraint.ENV_HEADER_AFFINITY_CONTAINER_LABEL.length());
                    AffinityConstraintDefinition def = extractAffinitionConstraintDefinition(affinityDef);
                    if (def != null && !StringUtils.isEmpty(def.key)) {
                        constraints.add(new ContainerLabelAffinityConstraint(def, allocatorDao));
                    }

                } else if (affinityDef.startsWith(ContainerAffinityConstraint.ENV_HEADER_AFFINITY_CONTAINER)) {
                    affinityDef = affinityDef.substring(ContainerAffinityConstraint.ENV_HEADER_AFFINITY_CONTAINER.length());
                    AffinityConstraintDefinition def = extractAffinitionConstraintDefinition(affinityDef);
                    if (def != null && !StringUtils.isEmpty(def.value)) {
                        constraints.add(new ContainerAffinityConstraint(def, objectManager, instanceDao));
                    }

                } else if (affinityDef.startsWith(HostAffinityConstraint.ENV_HEADER_AFFINITY_HOST_LABEL)) {
                    affinityDef = affinityDef.substring(HostAffinityConstraint.ENV_HEADER_AFFINITY_HOST_LABEL.length());
                    AffinityConstraintDefinition def = extractAffinitionConstraintDefinition(affinityDef);
                    if (def != null && !StringUtils.isEmpty(def.key)) {
                        constraints.add(new HostAffinityConstraint(def, allocatorDao));
                    }

                }
            }
        }
        return constraints;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private List<Constraint> extractConstraintsFromLabels(Map labels) {
        List<Constraint> constraints = new ArrayList<Constraint>();
        if (labels != null) {
            Set<String> affinityDefinitions = labels.keySet();
            for (String affinityDef : affinityDefinitions) {
                if (affinityDef == null) {
                    continue;
                }

                if (affinityDef.startsWith(ContainerLabelAffinityConstraint.LABEL_HEADER_AFFINITY_CONTAINER_LABEL)) {
                    affinityDef = affinityDef.substring(ContainerLabelAffinityConstraint.LABEL_HEADER_AFFINITY_CONTAINER_LABEL.length());
                    AffinityConstraintDefinition def = extractAffinitionConstraintDefinition(affinityDef);
                    if (def != null && !StringUtils.isEmpty(def.key)) {
                        constraints.add(new ContainerLabelAffinityConstraint(def, allocatorDao));
                    }

                } else if (affinityDef.startsWith(ContainerAffinityConstraint.LABEL_HEADER_AFFINITY_CONTAINER)) {
                    affinityDef = affinityDef.substring(ContainerAffinityConstraint.LABEL_HEADER_AFFINITY_CONTAINER.length());
                    AffinityConstraintDefinition def = extractAffinitionConstraintDefinition(affinityDef);
                    if (def != null && !StringUtils.isEmpty(def.value)) {
                        constraints.add(new ContainerAffinityConstraint(def, objectManager, instanceDao));
                    }

                } else if (affinityDef.startsWith(HostAffinityConstraint.LABEL_HEADER_AFFINITY_HOST_LABEL)) {
                    affinityDef = affinityDef.substring(HostAffinityConstraint.LABEL_HEADER_AFFINITY_HOST_LABEL.length());
                    AffinityConstraintDefinition def = extractAffinitionConstraintDefinition(affinityDef);
                    if (def != null && !StringUtils.isEmpty(def.key)) {
                        constraints.add(new HostAffinityConstraint(def, allocatorDao));
                    }

                }
            }
        }
        return constraints;
    }

    private AffinityConstraintDefinition extractAffinitionConstraintDefinition(String definitionString) {
        for (AffinityOps op : AffinityOps.values()) {
            int i = definitionString.indexOf(op.symbol);
            if (i != -1) {
                String key = definitionString.substring(0, i);
                String value = definitionString.substring(i + op.symbol.length());
                return new AffinityConstraintDefinition(op, key, value);
            }
        }
        return null;
    }

    @Override
    public boolean isCritical() {
        return false;
    }
}
