package io.cattle.platform.allocator.service.impl;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.allocator.constraint.Constraint;
import io.cattle.platform.allocator.constraint.PortsConstraint;
import io.cattle.platform.allocator.constraint.ValidHostsConstraint;
import io.cattle.platform.allocator.constraint.provider.AllocationConstraintsProvider;
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
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.metadata.MetadataManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.*;

public class AllocatorServiceImpl implements AllocatorService {

    private static final Logger log = LoggerFactory.getLogger(AllocatorServiceImpl.class);

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
        if (instance.getHostId() != null) {
            if (!portManager.optionallyAssignPorts(instance.getClusterId(), instance.getHostId(), instance.getId(), PortSpec.getPorts(instance))) {
                throw new FailedToAllocate(String.format("Error reserving ports: %s", extractPorts(instance)));
            }
        }
    }

    private String extractPorts(Instance instance) {
        List<String> ports = new ArrayList<>();
        for (PortSpec port : PortSpec.getPorts(instance)) {
            if (StringUtils.isBlank(port.getIpAddress())) {
                ports.add(String.format("%d", port.getPublicPort()));
            } else {
                ports.add(String.format("%s:%s", port.getIpAddress(), port.getPublicPort()));
            }
        }

        return StringUtils.join(ports, ", ");
    }

    @Override
    public void ensureResourcesReleasedForStop(Instance instance) {
        if (instance.getHostId() != null) {
            releaseResources(instance);
        }
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
                    }).collect(toList());
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
                throw new FailedToAllocate(String.format("%s", toErrorMessage(finalFailedConstraints)));
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

        Iterator<HostInfo> iter = allocationHelper.iterateHosts(options, null);
        return new Iterator<AllocationCandidate>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public AllocationCandidate next() {
                HostInfo host = iter.next();
                return new AllocationCandidate(host.getId(), host.getUuid(), options.getClusterId());
            }
        };
    }

    protected void releaseAllocation(Instance instance) {
        Host host = objectManager.loadResource(Host.class, instance.getHostId());
        if (host == null) {
            return;
        }

        allocatorDao.releaseAllocation(instance);
        ObjectUtils.publishChanged(eventService, objectManager, host);
    }

    protected boolean recordCandidate(AllocationAttempt attempt, AllocationCandidate candidate) {
        return allocatorDao.recordCandidate(attempt, candidate, portManager);
    }

    private void releaseResources(Instance instance) {
        if (instance.getHostId() != null) {
            portManager.releasePorts(instance.getClusterId(), instance.getHostId(), instance.getId(), PortSpec.getPorts(instance));
        }
    }

}
