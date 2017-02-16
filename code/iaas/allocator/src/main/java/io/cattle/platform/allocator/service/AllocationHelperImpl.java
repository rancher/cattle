package io.cattle.platform.allocator.service;

import static io.cattle.platform.core.model.tables.ServiceTable.*;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.agent.instance.dao.AgentInstanceDao;

import io.cattle.platform.allocator.constraint.AffinityConstraintDefinition;
import io.cattle.platform.allocator.constraint.AffinityConstraintDefinition.AffinityOps;
import io.cattle.platform.allocator.constraint.Constraint;
import io.cattle.platform.allocator.constraint.ContainerAffinityConstraint;
import io.cattle.platform.allocator.constraint.ContainerLabelAffinityConstraint;
import io.cattle.platform.allocator.constraint.HostAffinityConstraint;
import io.cattle.platform.allocator.dao.AllocatorDao;
import io.cattle.platform.allocator.exception.FailedToAllocate;
import io.cattle.platform.allocator.lock.AllocateConstraintLock;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.dao.LabelsDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.eventing.exception.EventExecutionException;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.jooq.tools.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AllocationHelperImpl implements AllocationHelper {
    
    private static final Logger log = LoggerFactory.getLogger(AllocationHelperImpl.class);

    private static final String SERVICE_NAME_MACRO = "${service_name}";
    private static final String STACK_NAME_MACRO = "${stack_name}";

    // LEGACY: Temporarily support ${project_name} but this has become ${stack_name} now
    private static final String PROJECT_NAME_MACRO = "${project_name}";

    // TODO: We should refactor since these are defined in ServiceDiscoveryConstants too
    private static final String LABEL_STACK_NAME = "io.rancher.stack.name";
    private static final String LABEL_STACK_SERVICE_NAME = "io.rancher.stack_service.name";
    private static final String LABEL_PROJECT_SERVICE_NAME = "io.rancher.project_service.name";
    private static final String LABEL_SERVICE_LAUNCH_CONFIG = "io.rancher.service.launch.config";
    private static final String PRIMARY_LAUNCH_CONFIG_NAME = "io.rancher.service.primary.launch.config";
    private static final String SCHEDULER_PRIORITIZE_EVENT = "scheduler.prioritize";
    private static final String SCHEDULER_PRIORITIZE_RESPONSE = "prioritizedCandidates";
    private static final String CONTEXT = "context";
    private static final String SCHEDULER_REQUEST_DATA_NAME = "schedulerRequest";

    @Inject
    AgentInstanceDao agentInstanceDao;

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
    AgentLocator agentLocator;
    
    @Override
    public List<Long> getAllHostsSatisfyingHostAffinity(Long accountId, Map<String, String> labelConstraints) {
        return getHostsSatisfyingHostAffinityInternal(true, accountId, labelConstraints);
    }

    @Override
    public List<Long> getHostsSatisfyingHostAffinity(Long accountId, Map<String, String> labelConstraints) {
        return getHostsSatisfyingHostAffinityInternal(false, accountId, labelConstraints);
    }

    protected List<Long> getHostsSatisfyingHostAffinityInternal(boolean includeRemoved, Long accountId, Map<String, String> labelConstraints) {
        List<? extends Host> hosts = includeRemoved ? allocatorDao.getNonPurgedHosts(accountId) : allocatorDao.getActiveHosts(accountId);
       
        List<Constraint> hostAffinityConstraints = getHostAffinityConstraintsFromLabels(labelConstraints);

        List<Long> acceptableHostIds = new ArrayList<Long>();
        List<String> acceptableHostUUIDsFromExSche = callExternalSchedulerForHost(accountId, labelConstraints);
        for (Host host : hosts) {
            if (hostSatisfiesHostAffinity(host.getId(), hostAffinityConstraints)) {
                if (acceptableHostUUIDsFromExSche == null || acceptableHostUUIDsFromExSche.contains(host.getUuid())) {
                    acceptableHostIds.add(host.getId());
                }
            }
        }
        return acceptableHostIds;
    }
    
    @SuppressWarnings("unchecked")
    private List<String> callExternalSchedulerForHost(Long accountId, Map<String, String> labels) {
        List<Long> agentIds = agentInstanceDao.getAgentProvider(SystemLabels.LABEL_AGENT_SERVICE_SCHEDULING_PROVIDER, accountId);
        List<String> hosts = null;
        List<Object> instances = new ArrayList<>();
        Map<String, Object> instance = constructInstanceMapWithLabel(labels);
        instances.add(instance);
        for (Long agentId : agentIds) {
            EventVO<Map<String, Object>> schedulerEvent = buildEvent(SCHEDULER_PRIORITIZE_EVENT, instances);
            if (schedulerEvent != null) {
                RemoteAgent agent = agentLocator.lookupAgent(agentId);
                Event eventResult = callScheduler("Error getting hosts for resources for global service", schedulerEvent, agent);
                if (hosts == null) {
                    hosts = (List<String>) CollectionUtils.getNestedValue(eventResult.getData(), SCHEDULER_PRIORITIZE_RESPONSE);
                } else {
                    List<String> newHosts = (List<String>) CollectionUtils.getNestedValue(eventResult.getData(), SCHEDULER_PRIORITIZE_RESPONSE);
                    hosts.retainAll(newHosts);
                }

                if (hosts.isEmpty()) {
                    throw new FailedToAllocate("No healthy hosts meet the resource constraints for global service");
                }
            }
        }
        return hosts;
    }
    
    private Map<String, Object> constructInstanceMapWithLabel(Map<String, String> labels) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("labels", labels);
        Map<String, Object> data = new HashMap<>();
        data.put("fields", fields);
        Map<String, Object> instance = new HashMap<>();
        instance.put("data", data);
        return instance;
    }
    
    private EventVO<Map<String, Object>> buildEvent(String eventName, Object instances) {
        return newEvent(eventName, instances);
    }
    
    private EventVO<Map<String, Object>> newEvent(String eventName, Object context) {
        Map<String, Object> eventData = new HashMap<String, Object>();
        Map<String, Object> reqData = new HashMap<>();
        reqData.put(CONTEXT, context);
        eventData.put(SCHEDULER_REQUEST_DATA_NAME, reqData);
        EventVO<Map<String, Object>> schedulerEvent = EventVO.<Map<String, Object>> newEvent(eventName).withData(eventData);
        return schedulerEvent;
    }
    
    Event callScheduler(String message, EventVO<Map<String, Object>> schedulerEvent, RemoteAgent agent) {
        try {
            return agent.callSync(schedulerEvent);
        } catch (EventExecutionException e) {
            log.error("External scheduler replied with an error: {}", e.getMessage());
            throw new FailedToAllocate(message, e);
        }
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
            candidate.setHost(hostId);
            if (!constraint.matches(candidate)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void normalizeLabels(long stackId, Map<String, String> systemLabels, Map<String, String> serviceUserLabels) {
        String stackName = systemLabels.get(LABEL_STACK_NAME);
        String stackServiceNameWithLaunchConfig = systemLabels.get(LABEL_STACK_SERVICE_NAME);
        String launchConfig = systemLabels.get(LABEL_SERVICE_LAUNCH_CONFIG);

        Set<String> serviceNamesInStack = getServiceNamesInStack(stackId);

        for (Map.Entry<String, String> entry : serviceUserLabels.entrySet()) {
            String labelValue = entry.getValue();
            if (entry.getKey().startsWith(ContainerLabelAffinityConstraint.LABEL_HEADER_AFFINITY_CONTAINER_LABEL) &&
                    labelValue != null) {
                String userEnteredServiceName = null;
                if (labelValue.startsWith(LABEL_STACK_SERVICE_NAME)) {
                    userEnteredServiceName = labelValue.substring(LABEL_STACK_SERVICE_NAME.length() + 1);
                } else if (labelValue.startsWith(LABEL_PROJECT_SERVICE_NAME)) {
                    userEnteredServiceName = labelValue.substring(LABEL_PROJECT_SERVICE_NAME.length() + 1);
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
                    if (!PRIMARY_LAUNCH_CONFIG_NAME.equals(launchConfig) &&
                            stackServiceNameWithLaunchConfig.startsWith(userEnteredServiceName)) {
                        // automatically append secondary launchConfig
                        userEnteredServiceName = userEnteredServiceName + "/" + launchConfig;
                    }
                    entry.setValue(LABEL_STACK_SERVICE_NAME + "=" + userEnteredServiceName);
                }
            }
        }
    }

    // TODO: Fix repeated DB call even if DB's cache no longer hits the disk
    private Set<String> getServiceNamesInStack(long stackId) {
        Set<String> servicesInEnv = new HashSet<String>();

        List<? extends Service> services = objectManager.find(Service.class, SERVICE.STACK_ID, stackId, SERVICE.REMOVED,
                null);
        for (Service service : services) {
            servicesInEnv.add(service.getName().toLowerCase());
        }
        return servicesInEnv;
    }

    @Override
    public void mergeLabels(Map<String, String> srcMap, Map<String, String> destMap) {
        if (srcMap == null || destMap == null) {
            return;
        }
        for (Map.Entry<String, String> entry : srcMap.entrySet()) {
            String key = entry.getKey();
            if (key.toLowerCase().startsWith("io.rancher")) {
                key = key.toLowerCase();
            }
            String value = entry.getValue();
            if (key.startsWith("io.rancher.scheduler.affinity")) {
                // merge labels
                String destValue = destMap.get(key);
                if (StringUtils.isEmpty(destValue)) {
                    destMap.put(key, value);
                } else if (StringUtils.isEmpty(value)) {
                    continue;
                } else if (!destValue.toLowerCase().contains(value.toLowerCase())) {
                    destMap.put(key, destValue + "," + value);
                }
            } else {
                // overwrite label value
                destMap.put(key, value);
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
                        constraints.add(new HostAffinityConstraint(def, allocatorDao));
                    }

                } else if (affinityDef.startsWith(ContainerLabelAffinityConstraint.ENV_HEADER_AFFINITY_CONTAINER_LABEL)) {
                    affinityDef = affinityDef.substring(ContainerLabelAffinityConstraint.ENV_HEADER_AFFINITY_CONTAINER_LABEL.length());
                    AffinityConstraintDefinition def = extractAffinitionConstraintDefinitionFromEnv(affinityDef);
                    if (def != null && !StringUtils.isEmpty(def.getKey())) {
                        constraints.add(new ContainerLabelAffinityConstraint(def, allocatorDao));
                    }
                }
            }
        }
        return constraints;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<Constraint> extractConstraintsFromLabels(Map labels, Instance instance) {
        List<Constraint> constraints = new ArrayList<Constraint>();
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
     * ${stack_name}
     * LEGACY:
     * ${project_name}
     *
     * @param valueStr
     * @param instance
     * @return
     */
    @SuppressWarnings("unchecked")
    private String evaluateMacros(String valueStr, Instance instance) {
        if (valueStr.indexOf(SERVICE_NAME_MACRO) != -1 ||
                valueStr.indexOf(STACK_NAME_MACRO) != -1 ||
                valueStr.indexOf(PROJECT_NAME_MACRO) != -1) {

            Map<String, String> labels = DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_LABELS).as(Map.class);
            String serviceLaunchConfigName = "";
            String stackName = "";
            if (labels != null && !labels.isEmpty()) {
                for (Map.Entry<String, String> label : labels.entrySet()) {
                    if (LABEL_STACK_NAME.equals(label.getKey())) {
                        stackName = label.getValue();
                    } else if (LABEL_STACK_SERVICE_NAME.equals(label.getKey())) {
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

                // LEGACY: ${project_name} rename ${stack_name}
                valueStr = valueStr.replace(PROJECT_NAME_MACRO, stackName);
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

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<LockDefinition> extractAllocationLockDefinitions(Instance instance) {
        Map env = DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_ENVIRONMENT).as(jsonMapper, Map.class);
        List<LockDefinition> lockDefs = extractAllocationLockDefinitionsFromEnv(env);

        Map labels = DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_LABELS).as(jsonMapper, Map.class);
        if (labels == null) {
            return lockDefs;
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
        List<LockDefinition> constraints = new ArrayList<LockDefinition>();
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
