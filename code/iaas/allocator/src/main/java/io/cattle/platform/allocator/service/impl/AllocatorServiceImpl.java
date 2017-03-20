package io.cattle.platform.allocator.service.impl;

import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.PortTable.*;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.agent.instance.dao.AgentInstanceDao;
import io.cattle.platform.allocator.constraint.AllocationConstraintsProvider;
import io.cattle.platform.allocator.constraint.Constraint;
import io.cattle.platform.allocator.constraint.PortsConstraint;
import io.cattle.platform.allocator.constraint.PortsConstraintProvider;
import io.cattle.platform.allocator.constraint.ValidHostsConstraint;
import io.cattle.platform.allocator.dao.AllocatorDao;
import io.cattle.platform.allocator.dao.impl.QueryOptions;
import io.cattle.platform.allocator.exception.FailedToAllocate;
import io.cattle.platform.allocator.lock.AllocateConstraintLock;
import io.cattle.platform.allocator.lock.AllocateResourceLock;
import io.cattle.platform.allocator.lock.AllocationBlockingMultiLock;
import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.allocator.service.AllocationHelper;
import io.cattle.platform.allocator.service.AllocationLog;
import io.cattle.platform.allocator.service.AllocatorService;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.VolumeDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.util.InstanceHelpers;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.docker.client.DockerImage;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.eventing.exception.EventExecutionException;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.metrics.util.MetricsUtil;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.util.type.CollectionUtils;
import io.cattle.platform.util.type.Named;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.netflix.config.DynamicStringProperty;

public class AllocatorServiceImpl implements AllocatorService, Named {

    private static final Logger log = LoggerFactory.getLogger(AllocatorServiceImpl.class);
    private static final DynamicStringProperty PORT_SCHEDULER_IMAGE_VERSION = ArchaiusUtil.getString("port.scheduler.image.version");
    private static final String FORCE_RESERVE = "force";
    private static final String HOST_ID = "hostID";
    private static final String RESOURCE_REQUESTS = "resourceRequests";
    private static final String CONTEXT = "context";
    private static final String SCHEDULER_REQUEST_DATA_NAME = "schedulerRequest";
    private static final String SCHEDULER_PRIORITIZE_EVENT = "scheduler.prioritize";
    private static final String SCHEDULER_RESERVE_EVENT = "scheduler.reserve";
    private static final String SCHEDULER_RELEASE_EVENT = "scheduler.release";
    private static final String SCHEDULER_PRIORITIZE_RESPONSE = "prioritizedCandidates";
    private static final String INSTANCE_RESERVATION = "instanceReservation";
    private static final String MEMORY_RESERVATION = "memoryReservation";
    private static final String CPU_RESERVATION = "cpuReservation";
    private static final String STORAGE_SIZE = "storageSize";
    private static final String PORT_RESERVATION = "portReservation";
    private static final String COMPUTE_POOL = "computePool";
    private static final String PORT_POOL = "portPool";
    private static final String BIND_ADDRESS = "bindAddress";
    private static final String PHASE = "phase";

    Timer allocateLockTimer = MetricsUtil.getRegistry().timer("allocator.allocate.with.lock");
    Timer allocateTimer = MetricsUtil.getRegistry().timer("allocator.allocate");
    Timer deallocateTimer = MetricsUtil.getRegistry().timer("allocator.deallocate");

    String name = getClass().getSimpleName();

    @Inject
    AgentInstanceDao agentInstanceDao;
    @Inject
    AgentLocator agentLocator;
    @Inject
    GenericMapDao mapDao;
    @Inject
    protected AllocatorDao allocatorDao;
    @Inject
    protected LockManager lockManager;
    @Inject
    protected ObjectManager objectManager;
    @Inject
    protected ObjectProcessManager processManager;
    @Inject
    AllocationHelper allocationHelper;
    @Inject
    VolumeDao volumeDao;
    @Inject
    protected List<AllocationConstraintsProvider> allocationConstraintProviders;

    @Override
    public void instanceAllocate(final Instance instance) {
        log.info("Allocating instance [{}]", instance.getId());
        lockManager.lock(new AllocateResourceLock(instance.getId()), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                allocateInstance(instance);
            }
        });
        log.info("Handled request for instance [{}]", instance.getId());
    }

    @Override
    public void instanceDeallocate(final Instance instance) {
        log.info("Deallocating instance [{}]", instance.getId());
        lockManager.lock(new AllocateResourceLock(instance.getId()), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                Context c = deallocateTimer.time();
                try {
                    if (assertDeallocated(instance.getId(), instance.getAllocationState(), "Instance")) {
                        return;
                    }

                    releaseAllocation(instance);
                } finally {
                    c.stop();
                }
            }
        });
        log.info("Handled request for instance [{}]", instance.getId());
    }

    @Override
    public void ensureResourcesReservedForStart(Instance instance) {
        List<Instance> instances = new ArrayList<>();
        instances.add(instance);
        List<Long> agentIds = getAgentResource(instance.getAccountId(), instances);
        String hostUuid = getHostUuid(instance);
        for (Long agentId: agentIds) {
            if (agentId != null && hostUuid != null) {
                EventVO<Map<String, Object>> schedulerEvent = buildEvent(SCHEDULER_RESERVE_EVENT, InstanceConstants.PROCESS_START,
                        instances, new HashSet<Volume>(), agentId);
                if (schedulerEvent != null) {
                    Map<String, Object> reqData = CollectionUtils.toMap(schedulerEvent.getData().get(SCHEDULER_REQUEST_DATA_NAME));
                    reqData.put(HOST_ID, hostUuid);
                    Long rhid = DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_REQUESTED_HOST_ID).as(Long.class);
                    if (rhid != null) {
                        reqData.put(FORCE_RESERVE, true);
                    }

                    RemoteAgent agent = agentLocator.lookupAgent(agentId);
                    Event eventResult = callScheduler("Error reserving resources: %s", schedulerEvent, agent);
                    if (eventResult.getData() == null) {
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void ensureResourcesReleasedForStop(Instance instance) {
        String hostUuid = getHostUuid(instance);
        if (hostUuid != null) {
            releaseResources(instance, hostUuid, InstanceConstants.PROCESS_STOP);
        }
    }

    @Override
    public void volumeDeallocate(Volume volume) {
        log.info("Deallocating volume [{}]", volume.getId());
        if (!allocatorDao.isAllocationReleased(volume)) {
            allocatorDao.releaseAllocation(volume);
            callExternalSchedulerToRelease(volume);
        }
        log.info("Handled request for volume [{}]", volume.getId());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> callExternalSchedulerForHostsSatisfyingLabels(Long accountId, Map<String, String> labels) {
        List<Long> agentIds = agentInstanceDao.getAgentProvider(SystemLabels.LABEL_AGENT_SERVICE_SCHEDULING_PROVIDER, accountId);
        List<String> hosts = null;
        List<Object> instances = new ArrayList<>();
        Map<String, Object> instance = constructInstanceMapWithLabel(labels);
        instances.add(instance);
        for (Long agentId : agentIds) {
            EventVO<Map<String, Object>> schedulerEvent = buildEvent(SCHEDULER_PRIORITIZE_EVENT, "globalServicePlanning", instances);
            if (schedulerEvent != null) {
                RemoteAgent agent = agentLocator.lookupAgent(agentId);
                Event eventResult = callScheduler("Error getting hosts for resources for global service", schedulerEvent, agent);
                if (hosts == null) {
                    hosts = (List<String>) CollectionUtils.getNestedValue(eventResult.getData(), SCHEDULER_PRIORITIZE_RESPONSE);
                } else {
                    List<String> newHosts = (List<String>) CollectionUtils.getNestedValue(eventResult.getData(), SCHEDULER_PRIORITIZE_RESPONSE);
                    hosts.retainAll(newHosts);
                }
            }
        }
        return hosts;
    }

    private EventVO<Map<String, Object>> buildEvent(String eventName, String phase, Object instances) {
        return newEvent(eventName, null, "instance", phase, null, instances);
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
    
    protected List<Instance> getInstancesToAllocate(Instance instance) {
        if (instance.getDeploymentUnitUuid() != null) {
            return allocatorDao.getUnmappedDeploymentUnitInstances(instance.getDeploymentUnitUuid());
        } else {
            List<Instance> instances = new ArrayList<>();
            instances.add(instance);
            return instances;
        }
    }

    protected void allocateInstance(final Instance origInstance) {
        final List<Instance> instances = getInstancesToAllocate(origInstance);
        final Set<Long> volumeIds = new HashSet<>();
        for (Instance instance : instances) {
            volumeIds.addAll(InstanceHelpers.extractVolumeIdsFromMounts(instance));
        }

        LockDefinition lock = getInstanceLockDef(origInstance, instances, volumeIds);
        if (lock != null) {
            Context c = allocateLockTimer.time();
            try {
                lockManager.lock(lock, new LockCallbackNoReturn() {
                    @Override
                    public void doWithLockNoResult() {
                        allocateInstanceInternal(origInstance, instances);
                    }
                });
            } finally {
                c.stop();
            }
        } else {
            allocateInstanceInternal(origInstance, instances);
        }
    }

    protected void allocateInstanceInternal(Instance origInstance, List<Instance> instances) {
        boolean origAllocated = assertAllocated(origInstance.getId(), origInstance.getAllocationState(), "Instance", true);
        for (Instance instance : instances) {
            boolean allocated = assertAllocated(origInstance.getId(), instance.getAllocationState(), "Instance", false);
            if (origAllocated ^ allocated) {
                throw new FailedToAllocate(String.format("Instance %s is in allocation state %s and instance %s is in allocation state %s.",
                        origInstance.getId(), origInstance.getAllocationState(), instance.getId(), instance.getAllocationState()));
            }
        }

        if (origAllocated) {
            return;
        }

        Host host = allocatorDao.getHost(origInstance);
        Long hostId = host != null ? host.getId() : null;

        Set<Volume> volumes = new HashSet<Volume>();
        Map<Volume, Set<StoragePool>> pools = new HashMap<Volume, Set<StoragePool>>();
        Long requestedHostId = null;

        for (Instance instance : instances) {
            volumes.addAll(objectManager.children(instance, Volume.class));
            volumes.addAll(InstanceHelpers.extractVolumesFromMounts(instance, objectManager));

            Long rhid = DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_REQUESTED_HOST_ID).as(Long.class);

            if (rhid != null) {
                if (requestedHostId == null) {
                    requestedHostId = rhid;
                } else if (!requestedHostId.equals(rhid)) {
                    throw new FailedToAllocate(String.format(
                            "Instances to allocate have conflicting requested host ids. Current instance id: %s. Requested host ids: %s, %s.",
                            instance.getId(), requestedHostId, rhid));
                }
            }
        }

        for (Volume v : volumes) {
            pools.put(v, new HashSet<StoragePool>(allocatorDao.getAssociatedPools(v)));
        }

        doAllocate(new AllocationAttempt(origInstance.getAccountId(), instances, hostId, requestedHostId, volumes, pools));
    }

    protected LockDefinition getInstanceLockDef(Instance origInstance, List<Instance> instances, Set<Long> volumeIds) {
        List<LockDefinition> locks = allocationHelper.extractAllocationLockDefinitions(origInstance);

        if (origInstance.getDeploymentUnitUuid() != null) {
            locks.add(new AllocateConstraintLock(AllocateConstraintLock.Type.DEPLOYMENT_UNIT, origInstance.getDeploymentUnitUuid()));
        }

        List<Long> instancesIds = DataAccessor.fieldLongList(origInstance, DockerInstanceConstants.FIELD_VOLUMES_FROM);
        Long networkFromId = DataAccessor.fieldLong(origInstance, DockerInstanceConstants.FIELD_NETWORK_CONTAINER_ID);
        if (networkFromId != null) {
            instancesIds.add(networkFromId);
        }
        for (Long id : instancesIds) {
            locks.add(new AllocateResourceLock(id));
        }

        for (Instance i : instances) {
            List<Port> ports = objectManager.find(Port.class, PORT.INSTANCE_ID, i.getId(), PORT.REMOVED, null);
            for (Port port : ports) {
                locks.add(new AllocateConstraintLock(AllocateConstraintLock.Type.PORT,
                        String.format("%s.%s", port.getProtocol(), port.getPublicPort())));
            }
        }

        List<? extends Volume> volsToLock = volumeDao.identifyUnmappedVolumes(origInstance.getAccountId(), volumeIds);
        for (Volume v : volsToLock) {
                locks.add(new AllocateConstraintLock(AllocateConstraintLock.Type.VOLUME, v.getId().toString()));
        }

        return locks.size() > 0 ? new AllocationBlockingMultiLock(locks) : null;
    }

    protected void doAllocate(final AllocationAttempt attempt) {
        AllocationLog log = new AllocationLog();
        populateConstraints(attempt, log);

        List<Constraint> finalFailedConstraints = new ArrayList<>();
        Context c = allocateTimer.time();
        try {
            do {
                Set<Constraint> failedConstraints = runAllocation(attempt);
                if (attempt.getMatchedCandidate() == null) {
                    boolean removed = false;
                    // iterate over failed constraints and remove first soft constraint if any
                    Iterator<Constraint> failedIter = failedConstraints.iterator();
                    while (failedIter.hasNext() && !removed) {
                        Constraint failedConstraint = failedIter.next();
                        if (failedConstraint.isHardConstraint()) {
                            continue;
                        }
                        attempt.getConstraints().remove(failedConstraint);
                        removed = true;
                    }
                    if (!removed) {
                        finalFailedConstraints.addAll(failedConstraints);
                        break;
                    }
                }
            } while (attempt.getMatchedCandidate() == null);
        } finally {
            c.stop();
        }

        if (attempt.getMatchedCandidate() == null) {
            if (finalFailedConstraints.size() > 0) {
                throw new FailedToAllocate(toErrorMessage(finalFailedConstraints));
            }
            throw new FailedToAllocate("Failed to find placement");
        }
    }

    protected String toErrorMessage(List<Constraint> constraints) {
        List<String> result = new ArrayList<>();
        for (Constraint c : constraints) {
            result.add(c.toString());
        }

        return StringUtils.join(result, ", ");
    }

    protected Set<Constraint> runAllocation(AllocationAttempt attempt) {
        logStart(attempt);

        List<Set<Constraint>> candidateFailedConstraintSets = new ArrayList<Set<Constraint>>();
        Iterator<AllocationCandidate> iter = getCandidates(attempt);
        try {
            boolean foundOne = false;
            while (iter.hasNext()) {
                foundOne = true;
                AllocationCandidate candidate = iter.next();
                Set<Constraint> failedConstraints = new HashSet<Constraint>();
                attempt.getCandidates().add(candidate);

                String prefix = String.format("[%s][%s]", attempt.getId(), candidate.getId());
                logCandidate(prefix, attempt, candidate);

                StringBuilder lg = new StringBuilder(String.format("%s Checking constraints:\n", prefix));
                boolean good = true;
                for (Constraint constraint : attempt.getConstraints()) {
                    boolean match = constraint.matches(candidate);
                    lg.append(String.format("  %s   constraint result [%s] : %s\n", prefix, match, constraint));
                    if (!match) {
                        good = false;
                        failedConstraints.add(constraint);
                    }
                }
                lg.append(String.format("  %s   candidate result  [%s]", prefix, good));
                log.info(lg.toString());
                if (good) {
                    if (recordCandidate(attempt, candidate)) {
                        attempt.setMatchedCandidate(candidate);
                        return failedConstraints;
                    } else {
                        log.info("{}   can not record result", prefix);
                    }
                }
                candidateFailedConstraintSets.add(failedConstraints);
            }
            if (!foundOne) {
                throw new FailedToAllocate("No healthy hosts with sufficient resources available");
            }
            return getWeakestConstraintSet(candidateFailedConstraintSets);
        } finally {
            if (iter != null) {
                close(iter);
            }
        }
    }

    // ideally we want zero hard constraints and the fewest soft constraints
    private Set<Constraint> getWeakestConstraintSet(List<Set<Constraint>> candidateFailedConstraintSets) {
        if (candidateFailedConstraintSets == null || candidateFailedConstraintSets.isEmpty()) {
            return Collections.emptySet();
        }
        Collections.sort(candidateFailedConstraintSets, new Comparator<Set<Constraint>>() {
            @Override
            public int compare(Set<Constraint> o1, Set<Constraint> o2) {
                if (o1 == o2) return 0;
                if (o1 != null && o2 == null) return 1;
                if (o1 == null && o2 != null) return -1;

                int[] o1NumOfHardAndSoftConstraints = getNumberOfConstraints(o1);
                int[] o2NumOfHardAndSoftConstraints = getNumberOfConstraints(o2);

                if (o1NumOfHardAndSoftConstraints[0] > o2NumOfHardAndSoftConstraints[0]) return 1;
                if (o1NumOfHardAndSoftConstraints[0] < o2NumOfHardAndSoftConstraints[0]) return -1;
                if (o1NumOfHardAndSoftConstraints[1] > o2NumOfHardAndSoftConstraints[1]) return 1;
                if (o1NumOfHardAndSoftConstraints[1] < o2NumOfHardAndSoftConstraints[1]) return -1;
                return 0;
            }

            private int[] getNumberOfConstraints(Set<Constraint> failedConstraints) {
                int hard = 0;
                int soft = 0;
                Iterator<Constraint> iter = failedConstraints.iterator();
                while (iter.hasNext()) {
                    Constraint c = iter.next();
                    if (c.isHardConstraint()) {
                        hard++;
                    } else {
                        soft++;
                    }
                }
                return new int[] { hard, soft };
            }

        });
        return candidateFailedConstraintSets.get(0);
    }

    protected void logCandidate(String prefix, AllocationAttempt attempt, AllocationCandidate candidate) {
        StringBuilder candidateLog = new StringBuilder(String.format("%s Checking candidate:\n", prefix));
        if (candidate.getHost() != null) {
            candidateLog.append(String.format("  %s   host [%s]\n", prefix, candidate.getHost()));
        }
        for (Map.Entry<Long, Set<Long>> entry : candidate.getPools().entrySet()) {
            candidateLog.append(String.format("  %s   volume [%s]\n", prefix, entry.getKey()));
            for (long poolId : entry.getValue()) {
                candidateLog.append(String.format("  %s   pool [%s]\n", prefix, poolId));
            }
        }
        for (Map.Entry<Long, Long> entry : candidate.getSubnetIds().entrySet()) {
            candidateLog.append(String.format("  %s   nic [%s] subnet [%s]\n", prefix, entry.getKey(), entry.getValue()));
        }
        candidateLog.deleteCharAt(candidateLog.length() - 1); // Remove trailing newline
        log.info(candidateLog.toString());
    }

    protected void logStart(AllocationAttempt attempt) {
        String id = attempt.getId();
        StringBuilder candidateLog = new StringBuilder(String.format("[%s] Attempting allocation for:\n", id));
        if (attempt.getInstances() != null) {
            List<Long>instanceIds = new ArrayList<Long>();
            for (Instance i : attempt.getInstances()) {
                instanceIds.add(i.getId());
            }
            candidateLog.append(String.format("  [%s] instance [%s]\n", id, instanceIds));
        }
        for (Map.Entry<Volume, Set<StoragePool>> entry : attempt.getPools().entrySet()) {
            long volumeId = entry.getKey().getId();
            candidateLog.append(String.format("  [%s] volume [%s]\n", id, volumeId));
            for (StoragePool pool : entry.getValue()) {
                candidateLog.append(String.format("  [%s] pool [%s]\n", id, pool.getId()));
            }
        }

        candidateLog.append(String.format("  [%s] constraints:\n", id));
        for (Constraint constraint : attempt.getConstraints()) {
            candidateLog.append(String.format("  [%s]   %s\n", id, constraint));
        }
        candidateLog.deleteCharAt(candidateLog.length() - 1); // Remove trailing newline
        log.info(candidateLog.toString());
    }

    protected void close(Iterator<AllocationCandidate> iter) {
    }

    protected void populateConstraints(AllocationAttempt attempt, AllocationLog log) {
        List<Constraint> constraints = attempt.getConstraints();

        for (AllocationConstraintsProvider provider : allocationConstraintProviders) {
            if (attempt.getRequestedHostId() == null || provider.isCritical()) {
                if (provider instanceof PortsConstraintProvider  && !useLegacyPortAllocation(attempt.getAccountId(), attempt.getInstances())) {
                    continue;
                }
                provider.appendConstraints(attempt, log, constraints);
            }
        }
        Collections.sort(constraints, new Comparator<Constraint>() {
            @Override
            public int compare(Constraint o1, Constraint o2) {
                if (o1 == o2) return 0;
                if (o1 != null && o2 == null) return -1;
                if (o1 == null && o2 != null) return 1;
                if (o1.isHardConstraint() && o2.isHardConstraint()) return 0;
                if (o1.isHardConstraint() && !o2.isHardConstraint()) return -1;
                return 1;
            }
        });
    }

    public boolean assertAllocated(long resourceId, String state, String type, boolean raiseOnBadState) {
        if (CommonStatesConstants.ACTIVE.equals(state) || ("instance".equalsIgnoreCase(type)
                && objectManager.findAny(InstanceHostMap.class, INSTANCE_HOST_MAP.INSTANCE_ID, resourceId, INSTANCE_HOST_MAP.REMOVED, null) != null)){
            log.info("{} [{}] is already allocated", type, resourceId);
            return true;
        }else if (raiseOnBadState && !CommonStatesConstants.ACTIVATING.equals(state)) {
            throw new FailedToAllocate(String.format("Illegal allocation state: %s", state));
        }
        return false;
    }

    public static boolean assertDeallocated(long resourceId, String state, String logType) {
        if (CommonStatesConstants.INACTIVE.equals(state)) {
            log.info("{} [{}] is already deallocated", logType, resourceId);
            return true;
        } else if (!CommonStatesConstants.DEACTIVATING.equals(state)) {
            throw new FailedToAllocate(String.format("Illegal deallocation state: %s", state));
        }

        return false;
    }

    protected Iterator<AllocationCandidate> getCandidates(AllocationAttempt attempt) {
        List<Long> volumeIds = new ArrayList<Long>();
        for (Volume v : attempt.getVolumes()) {
            volumeIds.add(v.getId());
        }

        QueryOptions options = new QueryOptions();

        options.setAccountId(attempt.getAccountId());

        options.setRequestedHostId(attempt.getRequestedHostId());

        for (Constraint constraint : attempt.getConstraints()) {
            if (constraint instanceof ValidHostsConstraint) {
                options.getHosts().addAll(((ValidHostsConstraint) constraint).getHosts());
            }

            if (constraint instanceof PortsConstraint) {
                options.setIncludeUsedPorts(true);
            }
        }

        List<String> orderedHostUUIDs = null;
        if (attempt.getRequestedHostId() == null) {
            orderedHostUUIDs = callExternalSchedulerForHosts(attempt);
        }
        return allocatorDao.iteratorHosts(orderedHostUUIDs, volumeIds, options);
    }

    protected void releaseAllocation(Instance instance) {
        // This is kind of strange logic to remove deallocate for every instance host map, but in truth there will be only one ihm
        Map<String, List<InstanceHostMap>> maps = allocatorDao.getInstanceHostMapsWithHostUuid(instance.getId());
        for (Map.Entry<String, List<InstanceHostMap>> entry : maps.entrySet()) {
            for (InstanceHostMap map : entry.getValue()) {
                if (!allocatorDao.isAllocationReleased(map)) {
                    allocatorDao.releaseAllocation(instance, map);
                    releaseResources(instance, entry.getKey(), InstanceConstants.PROCESS_DEALLOCATE);
                }
            }
        }
    }

    protected boolean recordCandidate(AllocationAttempt attempt, AllocationCandidate candidate) {
        Long newHost = candidate.getHost();
        if (newHost != null) {
            callExternalSchedulerToReserve(attempt, candidate);
        }
        return allocatorDao.recordCandidate(attempt, candidate);
    }

    private String getHostUuid(Instance instance) {
        List<? extends InstanceHostMap> maps = mapDao.findNonRemoved(InstanceHostMap.class, Instance.class, instance.getId());
        if (maps.size() > 1) {
            throw new FailedToAllocate(
                    String.format("Instance %s has %d instance host maps. Cannot reserve resources.", instance.getId(), maps.size()));
        }
        if (maps.size() == 1) {
            Host h = objectManager.loadResource(Host.class, maps.get(0).getHostId());
            return h.getUuid();
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private void callExternalSchedulerToReserve(AllocationAttempt attempt, AllocationCandidate candidate) {
        List<Long> agentIds = getAgentResource(attempt.getAccountId(), attempt.getInstances());
        for (Long agentId : agentIds) {
            EventVO<Map<String, Object>> schedulerEvent = buildEvent(SCHEDULER_RESERVE_EVENT, InstanceConstants.PROCESS_ALLOCATE, attempt.getInstances(),
                    attempt.getVolumes(), agentId);
            if (schedulerEvent != null) {
                Map<String, Object> reqData = CollectionUtils.toMap(schedulerEvent.getData().get(SCHEDULER_REQUEST_DATA_NAME));
                reqData.put(HOST_ID, candidate.getHostUuid());

                if (attempt.getRequestedHostId() != null) {
                    reqData.put(FORCE_RESERVE, true);
                }

                RemoteAgent agent = agentLocator.lookupAgent(agentId);
                Event eventResult = callScheduler("Error reserving resources: %s", schedulerEvent, agent);
                if (eventResult.getData() == null) {
                    return;
                }
                
                List<Map<String, Object>> data = (List<Map<String, Object>>) CollectionUtils.getNestedValue(eventResult.getData(), PORT_RESERVATION);
                if (data != null) {
                    attempt.setAllocatedIPs(data);
                } 
            }
        }
    }

    private void releaseResources(Instance instance, String hostUuid, String process) {
        List<Long> agentIds = getAgentResource(instance);
        for (Long agentId : agentIds) {
            EventVO<Map<String, Object>> schedulerEvent = buildReleaseEvent(process, instance, agentId);
            if (schedulerEvent != null) {
                Map<String, Object> reqData = CollectionUtils.toMap(schedulerEvent.getData().get(SCHEDULER_REQUEST_DATA_NAME));
                reqData.put(HOST_ID, hostUuid);
                RemoteAgent agent = agentLocator.lookupAgent(agentId);
                callScheduler("Error releasing resources: %s", schedulerEvent, agent);
            }
        }
    }
    
    private void callExternalSchedulerToRelease(Volume volume) {
        String hostUuid = allocatorDao.getAllocatedHostUuid(volume);
        if (StringUtils.isEmpty(hostUuid)) {
            return;
        }
        List<Long> agentIds = getAgentResource(volume);
        for (Long agentId : agentIds) {
            EventVO<Map<String, Object>> schedulerEvent = buildReleaseEvent(VolumeConstants.PROCESS_DEALLOCATE, volume, agentId);
            if (schedulerEvent != null) {
                Map<String, Object> reqData = CollectionUtils.toMap(schedulerEvent.getData().get(SCHEDULER_REQUEST_DATA_NAME));
                reqData.put(HOST_ID, hostUuid);
                RemoteAgent agent = agentLocator.lookupAgent(agentId);
                callScheduler("Error releasing resources: %s", schedulerEvent, agent);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> callExternalSchedulerForHosts(AllocationAttempt attempt) {
        List<String> hosts = null;
        List<Long> agentIds = getAgentResource(attempt.getAccountId(), attempt.getInstances());
        for (Long agentId : agentIds) {
            EventVO<Map<String, Object>> schedulerEvent = buildEvent(SCHEDULER_PRIORITIZE_EVENT, InstanceConstants.PROCESS_ALLOCATE, attempt.getInstances(),
                    attempt.getVolumes(), agentId);
            if (schedulerEvent != null) {
                RemoteAgent agent = agentLocator.lookupAgent(agentId);
                Event eventResult = callScheduler("Error getting hosts for resources: %s", schedulerEvent, agent);
                if (hosts == null) {
                    hosts = (List<String>) CollectionUtils.getNestedValue(eventResult.getData(), SCHEDULER_PRIORITIZE_RESPONSE);
                } else {
                    List<String> newHosts = (List<String>) CollectionUtils.getNestedValue(eventResult.getData(), SCHEDULER_PRIORITIZE_RESPONSE);
                    hosts.retainAll(newHosts);
                }

                if (hosts.isEmpty()) {
                    throw new FailedToAllocate(String.format("No healthy hosts meet the resource constraints: %s", extractResourceRequests(schedulerEvent)));
                }
            }
        }
        return hosts;
    }

    Event callScheduler(String message, EventVO<Map<String, Object>> schedulerEvent, RemoteAgent agent) {
        try {
            return agent.callSync(schedulerEvent);
        } catch (EventExecutionException e) {
            log.error("External scheduler replied with an error: {}", e.getMessage());
            throw new FailedToAllocate(String.format(message, extractResourceRequests(schedulerEvent)), e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<ResourceRequest> extractResourceRequests(EventVO<Map<String, Object>> schedulerEvent) {
        return  (List<ResourceRequest>)((Map<String, Object>)schedulerEvent.getData().get(SCHEDULER_REQUEST_DATA_NAME)).get(RESOURCE_REQUESTS);
    }

    private EventVO<Map<String, Object>> buildReleaseEvent(String phase, Object resource, Long agentId) {
        List<ResourceRequest> resourceRequests = new ArrayList<>();
        if (resource instanceof Instance) {
            String schedulerVersion = getSchedulerVersion(agentId);
            addInstanceResourceRequests(resourceRequests, (Instance)resource, schedulerVersion);
        }

        if (resourceRequests.isEmpty()) {
            return null;
        }

        return newEvent(SCHEDULER_RELEASE_EVENT, resourceRequests, resource.getClass().getSimpleName(), phase, ObjectUtils.getId(resource), null);
    }

    private EventVO<Map<String, Object>> buildEvent(String eventName, String phase, List<Instance> instances, Set<Volume> volumes, Long agentId) {
        List<ResourceRequest> resourceRequests = gatherResourceRequests(instances, volumes, agentId);
        if (resourceRequests.isEmpty()) {
            return null;
        }

        return newEvent(eventName, resourceRequests, "instance", phase, instances.get(0).getId(), instances);
    }

    private EventVO<Map<String, Object>> newEvent(String eventName, List<ResourceRequest> resourceRequests, String resourceType, String phase,
            Object resourceId, Object context) {
        Map<String, Object> eventData = new HashMap<String, Object>();
        Map<String, Object> reqData = new HashMap<>();
        if (resourceRequests != null) {
            reqData.put(RESOURCE_REQUESTS, resourceRequests);
        }
        reqData.put(CONTEXT, context);
        reqData.put(PHASE, phase);
        eventData.put(SCHEDULER_REQUEST_DATA_NAME, reqData);
        EventVO<Map<String, Object>> schedulerEvent = EventVO.<Map<String, Object>> newEvent(eventName).withData(eventData);
        schedulerEvent.setResourceType(resourceType);
        if (resourceId != null) {
            schedulerEvent.setResourceId(resourceId.toString());
        }
        return schedulerEvent;
    }

    private List<ResourceRequest> gatherResourceRequests(List<Instance> instances, Set<Volume> volumes, Long agentId) {
        List<ResourceRequest> requests = new ArrayList<>();
        String schedulerVersion = getSchedulerVersion(agentId);
        for (Instance instance : instances) {
            addInstanceResourceRequests(requests, instance, schedulerVersion);
        }

        addVolumeResourceRequests(requests, volumes.toArray(new Volume[volumes.size()]));
        return requests;
    }

    private void addVolumeResourceRequests(List<ResourceRequest> requests, Volume... volumes) {
        for (Volume v : volumes) {
            if (v.getSizeMb() != null) {
                ResourceRequest rr = new ComputeResourceRequest(STORAGE_SIZE, v.getSizeMb(), COMPUTE_POOL);
                requests.add(rr);
            }
        }
    }

    private void addInstanceResourceRequests(List<ResourceRequest> requests, Instance instance, String schedulerVersion) {
        ResourceRequest memoryRequest = populateResourceRequestFromInstance(instance, MEMORY_RESERVATION, COMPUTE_POOL, schedulerVersion);
        if (memoryRequest != null) {
            requests.add(memoryRequest);
        }

        ResourceRequest cpuRequest = populateResourceRequestFromInstance(instance, CPU_RESERVATION, COMPUTE_POOL, schedulerVersion);
        if (cpuRequest != null) {
            requests.add(cpuRequest);
        }
        
        ResourceRequest portRequests = populateResourceRequestFromInstance(instance, PORT_RESERVATION, PORT_POOL, schedulerVersion);
        if (portRequests != null) {
            requests.add(portRequests);
        }

        ResourceRequest instanceRequest = populateResourceRequestFromInstance(instance, INSTANCE_RESERVATION, COMPUTE_POOL, schedulerVersion);
        requests.add(instanceRequest);
    }

    private List<Long> getAgentResource(Long accountId, List<Instance> instances) {
        List<Long> agentIds = agentInstanceDao.getAgentProvider(SystemLabels.LABEL_AGENT_SERVICE_SCHEDULING_PROVIDER, accountId);
        for (Instance instance : instances) {
            if (agentIds.contains(instance.getAgentId())) {
                return new ArrayList<>();
            }
        }
        return agentIds;
    }

    private List<Long> getAgentResource(Object resource) {
        Long accountId = (Long)ObjectUtils.getAccountId(resource);
        List<Long> agentIds = agentInstanceDao.getAgentProvider(SystemLabels.LABEL_AGENT_SERVICE_SCHEDULING_PROVIDER, accountId);
        // If the resource being allocated is a scheduling provider agent, return null so that we don't try to send the container to the scheduler.
        Long resourceAgentId = (Long)ObjectUtils.getPropertyIgnoreErrors(resource, "agentId");
        if (resourceAgentId != null && agentIds.contains(resourceAgentId)) {
            return new ArrayList<>();
        }
        return agentIds;
    }

    private ResourceRequest populateResourceRequestFromInstance(Instance instance, String resourceType, String poolType, String schedulerVersion) {
        switch (resourceType) {
        case PORT_RESERVATION:
            if (useLegacyPortAllocation(schedulerVersion)) {
                return null;
            }
            PortBindingResourceRequest request = new PortBindingResourceRequest();
            request.setResource(resourceType);
            request.setInstanceId(instance.getId().toString());
            request.setResourceUuid(instance.getUuid());
            List<PortSpec> portReservation = new ArrayList<>();
            for(Port port: objectManager.children(instance, Port.class)) {
                PortSpec spec = new PortSpec();
                String bindAddress = DataAccessor.fieldString(port, BIND_ADDRESS);
                if (bindAddress != null) {
                    spec.setIpAddress(bindAddress);
                }
                spec.setPrivatePort(port.getPrivatePort());
                spec.setPublicPort(port.getPublicPort());
                String proto = StringUtils.isEmpty(port.getProtocol()) ? "tcp" : port.getProtocol();
                spec.setProtocol(proto);
                portReservation.add(spec);
            }
            if (portReservation.isEmpty()) {
                return null;
            }
            request.setPortRequests(portReservation);
            request.setType(poolType);
            return request;
        case INSTANCE_RESERVATION:
            return new ComputeResourceRequest(INSTANCE_RESERVATION, 1l, poolType);
        case MEMORY_RESERVATION:
            if (instance.getMemoryReservation() != null && instance.getMemoryReservation() > 0) {
                ResourceRequest rr = new ComputeResourceRequest(MEMORY_RESERVATION, instance.getMemoryReservation(), poolType);
                return rr;
            } 
            return null;
        case CPU_RESERVATION:
            if (instance.getMilliCpuReservation() != null && instance.getMilliCpuReservation() > 0) {
                ResourceRequest rr = new ComputeResourceRequest(CPU_RESERVATION, instance.getMilliCpuReservation(), poolType);
                return rr;
            }
            return null;
        }
        return null;
    }

    protected boolean useLegacyPortAllocation(Long accountId, List<Instance> instances) {
        List<Long> agentIds = getAgentResource(accountId, instances);
        if (agentIds == null || agentIds.size() == 0) {
            return true;
        }
        return useLegacyPortAllocation(agentIds);
    }

    protected boolean useLegacyPortAllocation(List<Long> agentIds) {
        for(Long agentId: agentIds) {
            String schedulerVersion = getSchedulerVersion(agentId);
            if (useLegacyPortAllocation(schedulerVersion)) {
                return true;
            }
        }
        return false;
    }

    protected String getSchedulerVersion(Long agentId) {
        Instance instance = agentInstanceDao.getInstanceByAgent(agentId);
        String imageUuid = (String) DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_IMAGE_UUID).get();
        DockerImage img = DockerImage.parse(imageUuid);
        String[] imageParts = img.getFullName().split(":");
        if (imageParts.length <= 1) {
            return "";
        }
        if (!imageParts[0].equals("rancher/scheduler")) {
            return "NotApplicable";
        }
        return imageParts[imageParts.length - 1];
    }

    protected boolean useLegacyPortAllocation(String actualVersion) {
        String requiredVersion = PORT_SCHEDULER_IMAGE_VERSION.get();
        if (StringUtils.isEmpty(requiredVersion)) {
            // Property not available. Use legacy
            return true;
        }
        String[] requiredParts = requiredVersion.split("\\.");
        if (requiredParts.length < 3) {
            // Required image is not following semantic versioning. Assume custom, don't use legacy
            return false;
        }
        int requiredMajor, requiredMinor = 0;
        try {
            String majorTemp = requiredParts[0].startsWith("v") ? requiredParts[0].substring(1, requiredParts[0].length()) : requiredParts[0];
            requiredMajor = Integer.valueOf(majorTemp);
            requiredMinor = Integer.valueOf(requiredParts[1]);
        } catch (NumberFormatException e) {
            // Require image is not following semantic versioning. Assume custom, don't use legacy
            return false;
        }

        String[] actualParts = actualVersion.split("\\.");
        if (actualParts.length < 3) {
            // Image is not following semantic versioning. Assume custom, don't use legacy
            return false;
        }

        int actualMajor, actualMinor = 0;
        try {
            String majorTemp = actualParts[0].startsWith("v") ? actualParts[0].substring(1, actualParts[0].length()) : actualParts[0];
            actualMajor = Integer.valueOf(majorTemp).intValue();
            actualMinor = Integer.valueOf(actualParts[1]).intValue();
        } catch (NumberFormatException e) {
            // Image is not following semantic versioning. Assume custom, don't use legacy
            return false;
        }

        if (actualMajor < requiredMajor) {
            return true;
        } else if (actualMajor == requiredMajor && actualMinor < requiredMinor) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public String getName() {
        return name;
    }
}
