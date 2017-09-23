package io.cattle.platform.allocator.service.impl;

import com.netflix.config.DynamicStringProperty;
import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.allocator.constraint.Constraint;
import io.cattle.platform.allocator.constraint.PortsConstraint;
import io.cattle.platform.allocator.constraint.ValidHostsConstraint;
import io.cattle.platform.allocator.constraint.provider.AllocationConstraintsProvider;
import io.cattle.platform.allocator.constraint.provider.PortsConstraintProvider;
import io.cattle.platform.allocator.dao.AllocatorDao;
import io.cattle.platform.allocator.exception.FailedToAllocate;
import io.cattle.platform.allocator.lock.AllocateConstraintLock;
import io.cattle.platform.allocator.lock.AllocateResourceLock;
import io.cattle.platform.allocator.lock.AllocationBlockingMultiLock;
import io.cattle.platform.allocator.port.PortManager;
import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.allocator.service.AllocationHelper;
import io.cattle.platform.allocator.service.AllocationLog;
import io.cattle.platform.allocator.service.AllocatorService;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.addon.PortInstance;
import io.cattle.platform.core.addon.metadata.HostInfo;
import io.cattle.platform.core.cache.QueryOptions;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.StorageDriverConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.AgentDao;
import io.cattle.platform.core.dao.VolumeDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.StorageDriver;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.util.InstanceHelpers;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.docker.client.DockerImage;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.exception.EventExecutionException;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.metadata.MetadataManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.util.type.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AllocatorServiceImpl implements AllocatorService {

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
    private static final String PHASE = "phase";

    AgentDao agentDao;
    AgentLocator agentLocator;
    AllocatorDao allocatorDao;
    LockManager lockManager;
    ObjectManager objectManager;
    ObjectProcessManager processManager;
    AllocationHelper allocationHelper;
    VolumeDao volumeDao;
    MetadataManager metadataManager;
    EventService eventService;
    PortManager portManager;
    List<AllocationConstraintsProvider> allocationConstraintProviders;

    public AllocatorServiceImpl(AgentDao agentDao, AgentLocator agentLocator, AllocatorDao allocatorDao, LockManager lockManager,
                                ObjectManager objectManager, ObjectProcessManager processManager, AllocationHelper allocationHelper, VolumeDao volumeDao,
                                MetadataManager metadataManager, EventService eventService, PortManager portManager,
                                AllocationConstraintsProvider... allocationConstraintProviders) {
        super();
        this.agentDao = agentDao;
        this.agentLocator = agentLocator;
        this.allocatorDao = allocatorDao;
        this.lockManager = lockManager;
        this.objectManager = objectManager;
        this.processManager = processManager;
        this.allocationHelper = allocationHelper;
        this.volumeDao = volumeDao;
        this.metadataManager = metadataManager;
        this.eventService = eventService;
        this.portManager = portManager;
        this.allocationConstraintProviders = Arrays.asList(allocationConstraintProviders);
    }

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
                if (instance.getHostId() == null) {
                    return;
                }

                releaseAllocation(instance);
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
                EventVO<Map<String, Object>, ?> schedulerEvent = buildEvent(SCHEDULER_RESERVE_EVENT, InstanceConstants.PROCESS_START,
                        instances, new HashSet<>(), agentId);
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

        if (instance.getHostId() != null) {
            if (!portManager.optionallyAssignPorts(instance.getClusterId(), instance.getHostId(), instance.getId(), getPorts(instance))) {
                throw new FailedToAllocate(String.format("Error reserving ports: %s", extractPorts(instance)));
            }
        }
    }

    private String extractPorts(Instance instance) {
        List<String> ports = new ArrayList<>();
        for (PortInstance port : getPorts(instance)) {
            if (StringUtils.isBlank(port.getIpAddress())) {
                ports.add(String.format("%d", port.getPublicPort()));
            } else {
                ports.add(String.format("%s:%s", port.getIpAddress(), port.getPublicPort()));
            }
        }

        return StringUtils.join(ports, ", ");
    }

    private List<PortInstance> getPorts(Instance instance) {
        return DataAccessor.fieldObjectList(instance, InstanceConstants.FIELD_PORT_BINDINGS, PortInstance.class);
    }

    @Override
    public void ensureResourcesReleasedForStop(Instance instance) {
        String hostUuid = getHostUuid(instance);
        if (hostUuid != null) {
            releaseResources(instance, hostUuid, InstanceConstants.PROCESS_STOP);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> callExternalSchedulerForHostsSatisfyingLabels(Long accountId, Map<String, String> labels) {
        List<Long> agentIds = metadataManager.getAgentProvider(SystemLabels.LABEL_AGENT_SERVICE_SCHEDULING_PROVIDER, accountId);
        List<String> hosts = null;
        List<Object> instances = new ArrayList<>();
        Map<String, Object> instance = constructInstanceMapWithLabel(labels);
        instances.add(instance);
        for (Long agentId : agentIds) {
            EventVO<Map<String, Object>, ?> schedulerEvent = buildEvent(SCHEDULER_PRIORITIZE_EVENT, "globalServicePlanning", instances);
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

    private EventVO<Map<String, Object>, ?> buildEvent(String eventName, String phase, Object instances) {
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
        if (instance.getDeploymentUnitId() != null) {
            return allocatorDao.getUnmappedDeploymentUnitInstances(instance.getDeploymentUnitId()).stream()
                    .map((i) -> {
                        /* We want to use the in memory instance, not the one read from the DB
                         * As as this point some data may not be committed
                         */
                        if (i.getId().equals(instance.getId())) {
                            return instance;
                        }
                        return i;
                    }).collect(Collectors.toList());
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
            lockManager.lock(lock, new LockCallbackNoReturn() {
                @Override
                public void doWithLockNoResult() {
                    allocateInstanceInternal(origInstance, instances);
                }
            });
        } else {
            allocateInstanceInternal(origInstance, instances);
        }
    }

    protected void allocateInstanceInternal(Instance origInstance, List<Instance> instances) {
        if (origInstance.getHostId() != null) {
            return;
        }

        Host host = objectManager.loadResource(Host.class, origInstance.getHostId());
        Long hostId = host != null ? host.getId() : null;

        Set<Volume> volumes = new HashSet<>();
        Long requestedHostId = null;

        for (Instance instance : instances) {
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

        doAllocate(new AllocationAttempt(origInstance.getClusterId(), instances, hostId, requestedHostId, volumes));
    }

    protected LockDefinition getInstanceLockDef(Instance origInstance, List<Instance> instances, Set<Long> volumeIds) {
        List<LockDefinition> locks = allocationHelper.extractAllocationLockDefinitions(origInstance, instances);

        if (origInstance.getDeploymentUnitId() != null) {
            locks.add(new AllocateConstraintLock(AllocateConstraintLock.Type.DEPLOYMENT_UNIT, origInstance.getDeploymentUnitId()));
        }

        List<Long> instancesIds = DataAccessor.fieldLongList(origInstance, InstanceConstants.FIELD_VOLUMES_FROM);
        Long networkFromId = DataAccessor.fieldLong(origInstance, InstanceConstants.FIELD_NETWORK_CONTAINER_ID);
        if (networkFromId != null) {
            instancesIds.add(networkFromId);
        }
        for (Long id : instancesIds) {
            locks.add(new AllocateResourceLock(id));
        }

        for (Instance i : instances) {
            List<PortSpec> ports = InstanceConstants.getPortSpecs(i);
            for (PortSpec port : ports) {
                locks.add(new AllocateConstraintLock(AllocateConstraintLock.Type.PORT,
                        String.format("%s.%s.%s", origInstance.getClusterId(), port.getProtocol(), port.getPublicPort())));
            }
        }

        List<? extends Volume> volsToLock = volumeDao.identifyUnmappedVolumes(origInstance.getAccountId(), volumeIds);
        for (Volume v : volsToLock) {
                locks.add(new AllocateConstraintLock(AllocateConstraintLock.Type.VOLUME, v.getId().toString()));
        }

        List<Volume> volumes = InstanceHelpers.extractVolumesFromMounts(origInstance, objectManager);
        for (Volume volume: volumes) {
            StorageDriver driver = objectManager.loadResource(StorageDriver.class, volume.getStorageDriverId());
            if (driver != null) {
                String accessMode = DataAccessor.fieldString(driver, StorageDriverConstants.FIELD_VOLUME_ACCESS_MODE);
                if (VolumeConstants.ACCESS_MODE_SINGLE_HOST_RW.equals(accessMode) || VolumeConstants.ACCESS_MODE_SINGLE_INSTANCE_RW.equals(accessMode)) {
                    locks.add(new AllocateConstraintLock(AllocateConstraintLock.Type.VOLUME, volume.getId().toString()));
                }
            }
        }

        return locks.size() > 0 ? new AllocationBlockingMultiLock(locks) : null;
    }

    protected void doAllocate(final AllocationAttempt attempt) {
        AllocationLog log = new AllocationLog();
        populateConstraints(attempt, log);

        List<Constraint> finalFailedConstraints = new ArrayList<>();
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

        if (attempt.getMatchedCandidate() == null) {
            if (finalFailedConstraints.size() > 0) {
                throw new FailedToAllocate(String.format("%s and resource constraints: %s",
                        toErrorMessage(finalFailedConstraints), attempt.getResourceRequests()));
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

        List<Set<Constraint>> candidateFailedConstraintSets = new ArrayList<>();
        Iterator<AllocationCandidate> iter = getCandidates(attempt);

        boolean foundOne = false;
        while (iter.hasNext()) {
            foundOne = true;
            AllocationCandidate candidate = iter.next();
            Set<Constraint> failedConstraints = new HashSet<>();
            attempt.getCandidates().add(candidate);

            String prefix = String.format("[%s][%s]", attempt.getId(), candidate.getId());
            logCandidate(prefix, candidate);

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
    }

    // ideally we want zero hard constraints and the fewest soft constraints
    private Set<Constraint> getWeakestConstraintSet(List<Set<Constraint>> candidateFailedConstraintSets) {
        if (candidateFailedConstraintSets == null || candidateFailedConstraintSets.isEmpty()) {
            return Collections.emptySet();
        }
        candidateFailedConstraintSets.sort(new Comparator<Set<Constraint>>() {
            @Override
            public int compare(Set<Constraint> o1, Set<Constraint> o2) {
                if (o1 == o2) return 0;
                if (o1 != null && o2 == null) return 1;
                if (o1 == null) return -1;

                int[] o1NumOfHardAndSoftConstraints = getNumberOfConstraints(o1);
                int[] o2NumOfHardAndSoftConstraints = getNumberOfConstraints(o2);

                int result = Integer.compare(o1NumOfHardAndSoftConstraints[0], o2NumOfHardAndSoftConstraints[0]);
                return result != 0 ? result : Integer.compare(o1NumOfHardAndSoftConstraints[1], o2NumOfHardAndSoftConstraints[1]);
            }

            private int[] getNumberOfConstraints(Set<Constraint> failedConstraints) {
                int hard = 0;
                int soft = 0;
                for (Constraint c : failedConstraints) {
                    if (c.isHardConstraint()) {
                        hard++;
                    } else {
                        soft++;
                    }
                }
                return new int[]{hard, soft};
            }

        });
        return candidateFailedConstraintSets.get(0);
    }

    protected void logCandidate(String prefix, AllocationCandidate candidate) {
        StringBuilder candidateLog = new StringBuilder(String.format("%s Checking candidate:\n", prefix));
        if (candidate.getHost() != null) {
            candidateLog.append(String.format("  %s   host [%s]\n", prefix, candidate.getHost()));
        }
        candidateLog.deleteCharAt(candidateLog.length() - 1); // Remove trailing newline
        log.info(candidateLog.toString());
    }

    protected void logStart(AllocationAttempt attempt) {
        String id = attempt.getId();
        StringBuilder candidateLog = new StringBuilder(String.format("[%s] Attempting allocation for:\n", id));
        if (attempt.getInstances() != null) {
            List<Long>instanceIds = new ArrayList<>();
            for (Instance i : attempt.getInstances()) {
                instanceIds.add(i.getId());
            }
            candidateLog.append(String.format("  [%s] instance [%s]\n", id, instanceIds));
        }
        for (Volume volume : attempt.getVolumes()) {
            long volumeId = volume.getId();
            candidateLog.append(String.format("  [%s] volume [%s]\n", id, volumeId));
        }

        candidateLog.append(String.format("  [%s] constraints:\n", id));
        for (Constraint constraint : attempt.getConstraints()) {
            candidateLog.append(String.format("  [%s]   %s\n", id, constraint));
        }
        candidateLog.deleteCharAt(candidateLog.length() - 1); // Remove trailing newline
        log.info(candidateLog.toString());
    }

    protected void populateConstraints(AllocationAttempt attempt, AllocationLog log) {
        List<Constraint> constraints = attempt.getConstraints();

        for (AllocationConstraintsProvider provider : allocationConstraintProviders) {
            if (attempt.getRequestedHostId() == null || provider.isCritical()) {
                if (provider instanceof PortsConstraintProvider  && !useLegacyPortAllocation(attempt.getClusterId(), attempt.getInstances())) {
                    continue;
                }
                provider.appendConstraints(attempt, log, constraints);
            }
        }
        constraints.sort((o1, o2) -> {
            if (o1 == o2) return 0;
            if (o1 != null && o2 == null) return -1;
            if (o1 == null) return 1;
            if (o1.isHardConstraint() && o2.isHardConstraint()) return 0;
            if (o1.isHardConstraint() && !o2.isHardConstraint()) return -1;
            return 1;
        });
    }

    protected Iterator<AllocationCandidate> getCandidates(AllocationAttempt attempt) {
        List<Long> volumeIds = new ArrayList<>();
        for (Volume v : attempt.getVolumes()) {
            volumeIds.add(v.getId());
        }

        QueryOptions options = new QueryOptions();
        options.setClusterId(attempt.getClusterId());
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

        Iterator<HostInfo> iter = allocationHelper.iterateHosts(options, orderedHostUUIDs);
        return new Iterator<AllocationCandidate>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public AllocationCandidate next() {
                HostInfo host = iter.next();
                return new AllocationCandidate(host.getId(), host.getUuid(), host.getPorts(), options.getClusterId());
            }
        };
    }

    protected void releaseAllocation(Instance instance) {
        Host host = objectManager.loadResource(Host.class, instance.getHostId());
        if (host == null) {
            return;
        }

        allocatorDao.releaseAllocation(instance);
        releaseResources(instance, host.getUuid(), InstanceConstants.PROCESS_DEALLOCATE);
        ObjectUtils.publishChanged(eventService, objectManager, host);
    }

    protected boolean recordCandidate(AllocationAttempt attempt, AllocationCandidate candidate) {
        Long newHost = candidate.getHost();
        if (newHost != null) {
            callExternalSchedulerToReserve(attempt, candidate);
        }
        return allocatorDao.recordCandidate(attempt, candidate, portManager);
    }

    private String getHostUuid(Instance instance) {
        Host h = objectManager.loadResource(Host.class, instance.getHostId());
        return h != null ? h.getUuid() : null;
    }

    @SuppressWarnings("unchecked")
    private void callExternalSchedulerToReserve(AllocationAttempt attempt, AllocationCandidate candidate) {
        List<Long> agentIds = getAgentResource(attempt.getClusterId(), attempt.getInstances());
        for (Long agentId : agentIds) {
            EventVO<Map<String, Object>, ?> schedulerEvent = buildEvent(SCHEDULER_RESERVE_EVENT, InstanceConstants.PROCESS_ALLOCATE, attempt.getInstances(),
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
            EventVO<Map<String, Object>, ?> schedulerEvent = buildReleaseEvent(process, instance, agentId);
            if (schedulerEvent != null) {
                Map<String, Object> reqData = CollectionUtils.toMap(schedulerEvent.getData().get(SCHEDULER_REQUEST_DATA_NAME));
                reqData.put(HOST_ID, hostUuid);
                List<Instance> instances = new ArrayList<>();
                if (!InstanceConstants.PROCESS_STOP.equals(process)) {
                    instances.add(instance);
                    reqData.put(CONTEXT, instances);
                }
                RemoteAgent agent = agentLocator.lookupAgent(agentId);
                callScheduler("Error releasing resources: %s", schedulerEvent, agent);
            }
        }

        if (instance.getHostId() != null) {
            portManager.releasePorts(instance.getClusterId(), instance.getHostId(),
                    DataAccessor.fieldObjectList(instance, InstanceConstants.FIELD_PORT_BINDINGS, PortInstance.class));
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> callExternalSchedulerForHosts(AllocationAttempt attempt) {
        List<String> hosts = null;
        List<Long> agentIds = getAgentResource(attempt.getClusterId(), attempt.getInstances());
        for (Long agentId : agentIds) {
            EventVO<Map<String, Object>, ?> schedulerEvent = buildEvent(SCHEDULER_PRIORITIZE_EVENT, InstanceConstants.PROCESS_ALLOCATE, attempt.getInstances(),
                    attempt.getVolumes(), agentId);
            List<ResourceRequest> requests = extractResourceRequests(schedulerEvent);
            attempt.setResourceRequests(requests);
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

    Event callScheduler(String message, EventVO<Map<String, Object>, ?> schedulerEvent, RemoteAgent agent) {
        try {
            return agent.callSync(schedulerEvent);
        } catch (EventExecutionException e) {
            log.error("External scheduler replied with an error: {}", e.getMessage());
            throw new FailedToAllocate(String.format(message, extractResourceRequests(schedulerEvent)), e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<ResourceRequest> extractResourceRequests(EventVO<Map<String, Object>, ?> schedulerEvent) {
        return  (List<ResourceRequest>)((Map<String, Object>)schedulerEvent.getData().get(SCHEDULER_REQUEST_DATA_NAME)).get(RESOURCE_REQUESTS);
    }

    private EventVO<Map<String, Object>, ?> buildReleaseEvent(String phase, Object resource, Long agentId) {
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

    private EventVO<Map<String, Object>, ?> buildEvent(String eventName, String phase, List<Instance> instances, Set<Volume> volumes, Long agentId) {
        List<ResourceRequest> resourceRequests = gatherResourceRequests(instances, volumes, agentId);
        if (resourceRequests.isEmpty()) {
            return null;
        }

        return newEvent(eventName, resourceRequests, "instance", phase, instances.get(0).getId(), instances);
    }

    private EventVO<Map<String, Object>, ?> newEvent(String eventName, List<ResourceRequest> resourceRequests, String resourceType, String phase,
            Object resourceId, Object context) {
        Map<String, Object> eventData = new HashMap<>();
        Map<String, Object> reqData = new HashMap<>();
        if (resourceRequests != null) {
            reqData.put(RESOURCE_REQUESTS, resourceRequests);
        }
        reqData.put(CONTEXT, context);
        reqData.put(PHASE, phase);
        eventData.put(SCHEDULER_REQUEST_DATA_NAME, reqData);
        EventVO<Map<String, Object>, ?> schedulerEvent = EventVO.<Map<String, Object>, Object> newEvent(eventName).withData(eventData);
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

    private List<Long> getAgentResource(Long clusterId, List<Instance> instances) {
        List<Long> agentIds = metadataManager.getAgentProvider(SystemLabels.LABEL_AGENT_SERVICE_SCHEDULING_PROVIDER, clusterId);
        for (Instance instance : instances) {
            if (agentIds.contains(instance.getAgentId())) {
                return new ArrayList<>();
            }
        }
        return agentIds;
    }

    private List<Long> getAgentResource(Object resource) {
        Long clusterId = ObjectUtils.getClusterId(resource);
        List<Long> agentIds = metadataManager.getAgentProvider(SystemLabels.LABEL_AGENT_SERVICE_SCHEDULING_PROVIDER, clusterId);
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
            List<PortSpec> portSpecs = InstanceConstants.getPortSpecs(instance);
            List<PortInstance> portBindings = DataAccessor.fieldObjectList(instance, InstanceConstants.FIELD_PORT_BINDINGS, PortInstance.class);

            if (portSpecs.isEmpty()) {
                return null;
            }

            /* Populate bind addresses */
            for (PortSpec spec : portSpecs) {
                for (PortInstance portBinding : portBindings) {
                    if (portBinding.matches(spec) && portBinding.getBindIpAddress() != null) {
                        spec.setIpAddress(portBinding.getBindIpAddress());
                    }
                }
            }

            request.setPortRequests(portSpecs);
            request.setType(poolType);
            return request;
        case INSTANCE_RESERVATION:
            return new ComputeResourceRequest(INSTANCE_RESERVATION, 1L, poolType);
        case MEMORY_RESERVATION:
            if (instance.getMemoryReservation() != null && instance.getMemoryReservation() > 0) {
                return new ComputeResourceRequest(MEMORY_RESERVATION, instance.getMemoryReservation(), poolType);
            }
            return null;
        case CPU_RESERVATION:
            if (instance.getMilliCpuReservation() != null && instance.getMilliCpuReservation() > 0) {
                return new ComputeResourceRequest(CPU_RESERVATION, instance.getMilliCpuReservation(), poolType);
            }
            return null;
        }
        return null;
    }

    protected boolean useLegacyPortAllocation(Long clusterId, List<Instance> instances) {
        List<Long> agentIds = getAgentResource(clusterId, instances);
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
        Instance instance = agentDao.getInstanceByAgent(agentId);
        String imageUuid = (String) DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_IMAGE).get();
        DockerImage img = DockerImage.parse(imageUuid);
        if (img == null) {
            return "";
        }
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
        int requiredMajor, requiredMinor;
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

        int actualMajor, actualMinor;
        try {
            String majorTemp = actualParts[0].startsWith("v") ? actualParts[0].substring(1, actualParts[0].length()) : actualParts[0];
            actualMajor = Integer.valueOf(majorTemp);
            actualMinor = Integer.valueOf(actualParts[1]);
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

}
