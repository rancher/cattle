package io.cattle.platform.allocator.service;

import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.PortTable.*;

import io.cattle.platform.allocator.constraint.AllocationConstraintsProvider;
import io.cattle.platform.allocator.constraint.Constraint;
import io.cattle.platform.allocator.dao.AllocatorDao;
import io.cattle.platform.allocator.exception.FailedToAllocate;
import io.cattle.platform.allocator.lock.AllocateConstraintLock;
import io.cattle.platform.allocator.lock.AllocateResourceLock;
import io.cattle.platform.allocator.lock.AllocationBlockingMultiLock;
import io.cattle.platform.allocator.service.AllocationAttempt.AllocationType;
import io.cattle.platform.allocator.service.AllocationRequest.Type;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.util.InstanceHelpers;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.metrics.util.MetricsUtil;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;

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

public abstract class AbstractAllocator implements Allocator {

    private static final Logger log = LoggerFactory.getLogger(AbstractAllocator.class);

    Timer allocateLockTimer = MetricsUtil.getRegistry().timer("allocator.allocate.with.lock");
    Timer allocateTimer = MetricsUtil.getRegistry().timer("allocator.allocate");
    Timer deallocateTimer = MetricsUtil.getRegistry().timer("allocator.deallocate");

    @Inject
    protected AllocatorDao allocatorDao;
    @Inject
    protected LockManager lockManager;
    @Inject
    protected ObjectManager objectManager;
    @Inject
    protected ObjectProcessManager processManager;
    @Inject AllocatorService allocatorService;
    protected List<AllocationConstraintsProvider> allocationConstraintProviders;

    @Override
    public void allocate(final AllocationRequest request) {
        lockManager.lock(new AllocateResourceLock(request), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                switch (request.getType()) {
                case INSTANCE:
                    allocateInstance(request);
                    break;
                case VOLUME:
                    allocateVolume(request);
                    break;
                }
            }
        });
    }

    @Override
    public void deallocate(final AllocationRequest request) {
        lockManager.lock(new AllocateResourceLock(request), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                Context c = deallocateTimer.time();
                try {
                    acquireLockAndDeallocate(request);
                } finally {
                    c.stop();
                }
            }
        });
    }

    protected void acquireLockAndDeallocate(final AllocationRequest request) {
        lockManager.lock(getAllocationLock(request, null), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                runDeallocation(request);
            }
        });
    }

    protected void runDeallocation(final AllocationRequest request) {
        switch (request.getType()) {
        case INSTANCE:
            deallocateInstance(request);
            break;
        case VOLUME:
            deallocateVolume(request);
            break;
        }
    }

    protected void deallocateInstance(final AllocationRequest request) {
        final Instance instance = objectManager.loadResource(Instance.class, request.getResourceId());
        if (assertDeallocated(request.getResourceId(), instance.getAllocationState(), "Instance")) {
            return;
        }

        releaseAllocation(instance);
    }

    protected abstract void releaseAllocation(Instance instance);

    protected abstract void releaseAllocation(Volume volume);

    protected List<Instance> getInstancesToAllocate(Instance instance) {
        if (instance.getDeploymentUnitUuid() != null) {
            return allocatorDao.getUnmappedDeploymentUnitInstances(instance.getDeploymentUnitUuid());
        } else {
            List<Instance> instances = new ArrayList<>();
            instances.add(instance);
            return instances;
        }
    }

    protected void allocateInstance(final AllocationRequest request) {
        final Instance origInstance = objectManager.loadResource(Instance.class, request.getResourceId());
        final List<Instance> instances = getInstancesToAllocate(origInstance);

        LockDefinition lock = getInstanceLockDef(origInstance, instances);
        if (lock != null) {
            Context c = allocateLockTimer.time();
            try {
                lockManager.lock(lock, new LockCallbackNoReturn() {
                    @Override
                    public void doWithLockNoResult() {
                        allocateInstanceInternal(request, origInstance, instances);
                    }
                });
            } finally {
                c.stop();
            }
        } else {
            allocateInstanceInternal(request, origInstance, instances);
        }
    }

    protected void allocateInstanceInternal(AllocationRequest  request, Instance origInstance, List<Instance> instances) {
        boolean origAllocated = assertAllocated(request.getResourceId(), origInstance.getAllocationState(), "Instance", true);
        for (Instance instance : instances) {
            boolean allocated = assertAllocated(request.getResourceId(), instance.getAllocationState(), "Instance", false);
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

        doAllocate(request, new AllocationAttempt(AllocationType.INSTANCE, origInstance.getAccountId(), instances, hostId, requestedHostId, volumes, pools));
    }

    protected LockDefinition getInstanceLockDef(Instance origInstance, List<Instance> instances) {
        List<LockDefinition> locks = allocatorService.extractAllocationLockDefinitions(origInstance);

        if (origInstance.getDeploymentUnitUuid() != null) {
            locks.add(new AllocateConstraintLock(AllocateConstraintLock.Type.DEPLOYMENT_UNIT, origInstance.getDeploymentUnitUuid()));
        }

        List<Long> instancesIds = DataAccessor.fieldLongList(origInstance, DockerInstanceConstants.FIELD_VOLUMES_FROM);
        Long networkFromId = DataAccessor.fieldLong(origInstance, DockerInstanceConstants.FIELD_NETWORK_CONTAINER_ID);
        if (networkFromId != null) {
            instancesIds.add(networkFromId);
        }
        for (Long id : instancesIds) {
            locks.add(new AllocateResourceLock(Type.INSTANCE, id));
        }

        for (Instance i : instances) {
            List<Port> ports = objectManager.find(Port.class, PORT.INSTANCE_ID, i.getId(), PORT.REMOVED, null);
            for (Port port : ports) {
                locks.add(new AllocateConstraintLock(AllocateConstraintLock.Type.PORT,
                        String.format("%s.%s", port.getProtocol(), port.getPublicPort())));
            }
        }

        return locks.size() > 0 ? new AllocationBlockingMultiLock(locks) : null;
    }

    protected void deallocateVolume(AllocationRequest request) {
        final Volume volume = objectManager.loadResource(Volume.class, request.getResourceId());
        if (assertDeallocated(request.getResourceId(), volume.getAllocationState(), "Volume")) {
            return;
        }

        releaseAllocation(volume);
    }

    protected void allocateVolume(AllocationRequest request) {
        Volume volume = objectManager.loadResource(Volume.class, request.getResourceId());
        if(assertAllocated(request.getResourceId(), volume.getAllocationState(), "Volume", true)) {
            return;
        }

        Set<Volume> volumes = new HashSet<Volume>();
        volumes.add(volume);

        Map<Volume, Set<StoragePool>> pools = new HashMap<Volume, Set<StoragePool>>();
        Set<StoragePool> associatedPools = new HashSet<StoragePool>(allocatorDao.getAssociatedPools(volume));
        pools.put(volume, associatedPools);

        AllocationAttempt attempt = new AllocationAttempt(AllocationType.VOLUME, volume.getAccountId(), null, null, null, volumes, pools);

        doAllocate(request, attempt);
    }

    protected void doAllocate(final AllocationRequest request, final AllocationAttempt attempt) {
        AllocationLog log = getLog(request);
        populateConstraints(attempt, log);

        List<Constraint> finalFailedConstraints = new ArrayList<>();
        Context c = allocateTimer.time();
        try {
            do {
                Set<Constraint> failedConstraints = runAllocation(request, attempt);
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

    protected Set<Constraint> runAllocation(AllocationRequest request, AllocationAttempt attempt) {
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
                    if (candidate.getHost() != null && request.getType() == Type.VOLUME) {
                        throw new IllegalStateException("Attempting to allocate hosts during a volume allocation");
                    }

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

    protected abstract boolean recordCandidate(AllocationAttempt attempt, AllocationCandidate candidate);

    protected abstract LockDefinition getAllocationLock(AllocationRequest request, AllocationAttempt attempt);

    protected AllocationLog getLog(AllocationRequest request) {
        return new AllocationLog();
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

    protected abstract Iterator<AllocationCandidate> getCandidates(AllocationAttempt instanceRequest);

    protected void populateConstraints(AllocationAttempt attempt, AllocationLog log) {
        List<Constraint> constraints = attempt.getConstraints();

        for (AllocationConstraintsProvider provider : allocationConstraintProviders) {
            if (attempt.getRequestedHostId() == null || provider.isCritical()) {
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

    @Inject
    public void setAllocationConstraintProviders(List<AllocationConstraintsProvider> allocationConstraintProviders) {
        this.allocationConstraintProviders = allocationConstraintProviders;
    }
}
