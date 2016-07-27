package io.cattle.platform.allocator.service;

import io.cattle.platform.allocator.constraint.AllocationConstraintsProvider;
import io.cattle.platform.allocator.constraint.Constraint;
import io.cattle.platform.allocator.dao.AllocatorDao;
import io.cattle.platform.allocator.exception.AllocationException;
import io.cattle.platform.allocator.lock.AllocateResourceLock;
import io.cattle.platform.allocator.lock.AllocateVolumesResourceLock;
import io.cattle.platform.allocator.service.AllocationRequest.Type;
import io.cattle.platform.allocator.util.AllocatorUtils;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.util.InstanceHelpers;
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
    protected List<AllocationConstraintsProvider> allocationConstraintProviders;

    @Override
    public void allocate(final AllocationRequest request) {
        lockManager.lock(new AllocateResourceLock(request), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                switch (request.getType()) {
                case INSTANCE:
                    allocateInstance(request);
                case VOLUME:
                    allocateVolume(request);
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
        case VOLUME:
            deallocateVolume(request);
        }
    }

    protected void deallocateInstance(final AllocationRequest request) {
        final Instance instance = objectManager.loadResource(Instance.class, request.getResourceId());
        Boolean stateCheck = AllocatorUtils.checkDeallocateState(request.getResourceId(), instance.getAllocationState(), "Instance");
        if (Boolean.TRUE.equals(stateCheck)) {
            return;
        } else if (Boolean.FALSE.equals(stateCheck)) {
            throw new AllocationException(String.format("Illegal deallocation state: %s", instance.getAllocationState()));
        }

        releaseAllocation(instance);
    }

    protected void releaseAllocation(Instance instance) {
        allocatorDao.releaseAllocation(instance);
    }

    protected void releaseAllocation(Volume volume) {
        allocatorDao.releaseAllocation(volume);
    }

    protected void allocateInstance(final AllocationRequest request) {
        final Instance instance = objectManager.loadResource(Instance.class, request.getResourceId());
        Boolean stateCheck = AllocatorUtils.checkAllocateState(request.getResourceId(), instance.getAllocationState(), "Instance");
        if (Boolean.TRUE.equals(stateCheck)) {
            return;
        } else if (Boolean.FALSE.equals(stateCheck)) {
            throw new AllocationException(String.format("Illegal allocation state: %s", instance.getAllocationState()));
        }

        final Set<Host> hosts = new HashSet<Host>(allocatorDao.getHosts(instance));
        final Set<Volume> volumes = new HashSet<Volume>(objectManager.children(instance, Volume.class));
        volumes.addAll(InstanceHelpers.extractVolumesFromMounts(instance, objectManager));
        final Map<Volume, Set<StoragePool>> pools = new HashMap<Volume, Set<StoragePool>>();

        for (Volume v : volumes) {
            pools.put(v, new HashSet<StoragePool>(allocatorDao.getAssociatedPools(v)));
        }

        final Set<Nic> nics = new HashSet<Nic>(objectManager.children(instance, Nic.class));
        final Map<Nic, Subnet> subnets = new HashMap<Nic, Subnet>();

        for (Nic n : nics) {
            Subnet subnet = objectManager.loadResource(Subnet.class, n.getSubnetId());
            if (subnet != null) {
                subnets.put(n, subnet);
            }
        }

        lockManager.lock(new AllocateVolumesResourceLock(volumes), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                AllocationAttempt attempt = new AllocationAttempt(instance.getAccountId(), instance, hosts, volumes, pools, nics, subnets);

                doAllocate(request, attempt, instance);
            }
        });
    }

    protected void deallocateVolume(AllocationRequest request) {
        final Volume volume = objectManager.loadResource(Volume.class, request.getResourceId());
        Boolean stateCheck = AllocatorUtils.checkDeallocateState(request.getResourceId(), volume.getAllocationState(), "Volume");
        if (Boolean.TRUE.equals(stateCheck)) {
            return;
        } else if (Boolean.FALSE.equals(stateCheck)) {
            throw new AllocationException(String.format("Illegal deallocation state: %s", volume.getAllocationState()));
        }

        releaseAllocation(volume);
    }

    protected void allocateVolume(AllocationRequest request) {
        Volume volume = objectManager.loadResource(Volume.class, request.getResourceId());
        Boolean stateCheck = AllocatorUtils.checkAllocateState(request.getResourceId(), volume.getAllocationState(), "Volume");
        if (Boolean.TRUE.equals(stateCheck)) {
            return;
        } else if (Boolean.FALSE.equals(stateCheck)) {
            throw new AllocationException(String.format("Illegal allocation state: %s", volume.getAllocationState()));
        }

        Set<Volume> volumes = new HashSet<Volume>();
        volumes.add(volume);

        Map<Volume, Set<StoragePool>> pools = new HashMap<Volume, Set<StoragePool>>();
        Set<StoragePool> associatedPools = new HashSet<StoragePool>(allocatorDao.getAssociatedPools(volume));
        pools.put(volume, associatedPools);

        AllocationAttempt attempt = new AllocationAttempt(volume.getAccountId(), null, new HashSet<Host>(), volumes, pools, null, null);

        doAllocate(request, attempt, volume);
    }

    protected void doAllocate(final AllocationRequest request, final AllocationAttempt attempt, Object deallocate) {
        AllocationLog log = getLog(request);
        populateConstraints(attempt, log);

        Context c = allocateLockTimer.time();
        try {
            acquireLockAndAllocate(request, attempt, deallocate);
        } finally {
            c.stop();
        }
    }

    protected void acquireLockAndAllocate(final AllocationRequest request, final AllocationAttempt attempt, Object deallocate) {
        final List<Constraint> finalFailedConstraints = new ArrayList<>();
        lockManager.lock(getAllocationLock(request, attempt), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
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
            }
        });

        if (attempt.getMatchedCandidate() == null) {
            if (finalFailedConstraints.size() > 0) {
                throw new AllocationException(toErrorMessage(finalFailedConstraints));
            }
            throw new AllocationException("Failed to find placement");
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

                boolean good = true;
                for (Constraint constraint : attempt.getConstraints()) {
                    boolean match = constraint.matches(attempt, candidate);
                    log.info("{}   checking candidate [{}] : {}", prefix, match, constraint);
                    if (!match) {
                        good = false;
                        failedConstraints.add(constraint);
                    }
                }

                log.info("{}   candidates result [{}]", prefix, good);
                if (good) {
                    if (candidate.getHosts().size() > 0 && request.getType() == Type.VOLUME) {
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
                throw new AllocationException("No candidates available");
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

    protected boolean recordCandidate(AllocationAttempt attempt, AllocationCandidate candidate) {
        return allocatorDao.recordCandidate(attempt, candidate);
    }

    protected abstract LockDefinition getAllocationLock(AllocationRequest request, AllocationAttempt attempt);

    protected AllocationLog getLog(AllocationRequest request) {
        return new AllocationLog();
    }

    protected void logCandidate(String prefix, AllocationAttempt attempt, AllocationCandidate candidate) {
        log.info("{} Checking candidate:", prefix);
        for (long hostId : candidate.getHosts()) {
            log.info("{}   host [{}]", prefix, hostId);
        }
        for (Map.Entry<Long, Set<Long>> entry : candidate.getPools().entrySet()) {
            log.info("{}   volume [{}]", prefix, entry.getKey());
            for (long poolId : entry.getValue()) {
                log.info("{}     pool [{}]", prefix, poolId);
            }
        }
        for (Map.Entry<Long, Long> entry : candidate.getSubnetIds().entrySet()) {
            log.info("{}   nic [{}] subnet [{}]", prefix, entry.getKey(), entry.getValue());
        }
    }

    protected void logStart(AllocationAttempt request) {
        String id = request.getId();
        log.info("[{}] Attemping allocation for:", id);
        if (request.getInstance() != null) {
            log.info("[{}]   instance [{}]", id, request.getInstance().getId());
        }
        for (Map.Entry<Volume, Set<StoragePool>> entry : request.getPools().entrySet()) {
            long volumeId = entry.getKey().getId();
            log.info("[{}]   volume [{}]", id, volumeId);
            for (StoragePool pool : entry.getValue()) {
                log.info("[{}]     pool [{}]", id, pool.getId());
            }
        }
        log.info("[{}] constraints:", id);
        for (Constraint constraint : request.getConstraints()) {
            log.info("[{}]   {}", id, constraint);
        }
    }

    protected void close(Iterator<AllocationCandidate> iter) {
    }

    protected abstract Iterator<AllocationCandidate> getCandidates(AllocationAttempt instanceRequest);

    protected void populateConstraints(AllocationAttempt attempt, AllocationLog log) {
        List<Constraint> constraints = attempt.getConstraints();

        Instance instance = attempt.getInstance();
        Long requestedHostId = null;
        if (instance != null) {
            requestedHostId = DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_REQUESTED_HOST_ID).as(Long.class);
        }

        for (AllocationConstraintsProvider provider : allocationConstraintProviders) {
            if (requestedHostId == null || provider.isCritical()) {
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

    @Inject
    public void setAllocationConstraintProviders(List<AllocationConstraintsProvider> allocationConstraintProviders) {
        this.allocationConstraintProviders = allocationConstraintProviders;
    }
}
