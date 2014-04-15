package io.cattle.platform.allocator.service;

import io.cattle.platform.allocator.constraint.AllocationConstraintsProvider;
import io.cattle.platform.allocator.constraint.Constraint;
import io.cattle.platform.allocator.dao.AllocatorDao;
import io.cattle.platform.allocator.exception.UnsupportedAllocation;
import io.cattle.platform.allocator.lock.AllocateResourceLock;
import io.cattle.platform.allocator.lock.AllocateVolumesResourceLock;
import io.cattle.platform.allocator.service.AllocationRequest.Type;
import io.cattle.platform.allocator.util.AllocatorUtils;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.metrics.util.MetricsUtil;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

public abstract class AbstractAllocator implements Allocator {

    private static final Logger log = LoggerFactory.getLogger(AbstractAllocator.class);

    Timer allocateTimer = MetricsUtil.getRegistry().timer("allocator.allocate");
    Timer deallocateTimer = MetricsUtil.getRegistry().timer("allocator.deallocate");

    AllocatorDao allocatorDao;
    LockManager lockManager;
    ObjectManager objectManager;
    ObjectProcessManager processManager;
    List<AllocationConstraintsProvider> allocationConstraintProviders;

    @Override
    public boolean allocate(final AllocationRequest request) {
        if ( ! supports(request) )
            return false;

        try {
            return lockManager.lock(new AllocateResourceLock(request), new LockCallback<Boolean>() {
                @Override
                public Boolean doWithLock() {
                    switch (request.getType()) {
                    case INSTANCE:
                        return allocateInstance(request);
                    case VOLUME:
                        return allocateVolume(request);
                    }

                    return false;
                }
            });
        } catch( UnsupportedAllocation e ) {
            log.info("Unsupported allocation for [{}] : {}", this, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean deallocate(final AllocationRequest request) {
        if ( ! supports(request) )
            return false;

        try {
            return lockManager.lock(new AllocateResourceLock(request), new LockCallback<Boolean>() {
                @Override
                public Boolean doWithLock() {
                    Context c = deallocateTimer.time();
                    try {
                        return acquireLockAndDeallocate(request);
                    } finally {
                        c.stop();
                    }
                }
            });
        } catch( UnsupportedAllocation e ) {
            log.info("Unsupported allocation for [{}] : {}", this, e.getMessage());
            return false;
        }
    }

    protected boolean acquireLockAndDeallocate(final AllocationRequest request) {
        return lockManager.lock(getAllocationLock(request, null), new LockCallback<Boolean>() {
            @Override
            public Boolean doWithLock() {
                return runDeallocation(request);
            }
        });
    }

    protected boolean runDeallocation(final AllocationRequest request) {
        switch (request.getType()) {
        case INSTANCE:
            return deallocateInstance(request);
        case VOLUME:
            return deallocateVolume(request);
        }

        return false;
    }

    protected boolean deallocateInstance(final AllocationRequest request) {
        final Instance instance = objectManager.loadResource(Instance.class, request.getResourceId());
        Boolean stateCheck = AllocatorUtils.checkDeallocateState(request.getResourceId(), instance.getAllocationState(), "Instance");
        if ( stateCheck != null ) {
            return stateCheck;
        }

        releaseAllocation(instance);

        return true;
    }

    protected void releaseAllocation(Instance instance) {
        allocatorDao.releaseAllocation(instance);
    }

    protected void releaseAllocation(Volume volume) {
        allocatorDao.releaseAllocation(volume);
    }

    protected boolean allocateInstance(final AllocationRequest request) {
        final Instance instance = objectManager.loadResource(Instance.class, request.getResourceId());
        Boolean stateCheck = AllocatorUtils.checkAllocateState(request.getResourceId(), instance.getAllocationState(), "Instance");
        if ( stateCheck != null ) {
            return stateCheck;
        }

        final Set<Host> hosts = new HashSet<Host>(allocatorDao.getHosts(instance));
        final Set<Volume> volumes = new HashSet<Volume>(objectManager.children(instance, Volume.class));
        final Map<Volume,Set<StoragePool>> pools = new HashMap<Volume, Set<StoragePool>>();

        for ( Volume v : volumes ) {
            pools.put(v, new HashSet<StoragePool>(allocatorDao.getAssociatedPools(v)));
        }

        final Set<Nic> nics = new HashSet<Nic>(objectManager.children(instance, Nic.class));
        final Map<Nic,Subnet> subnets = new HashMap<Nic, Subnet>();

        for ( Nic n : nics ) {
            Subnet subnet = objectManager.loadResource(Subnet.class, n.getSubnetId());
            if ( subnet != null ) {
                subnets.put(n, subnet);
            }
        }

        return lockManager.lock(new AllocateVolumesResourceLock(volumes), new LockCallback<Boolean>() {
            @Override
            public Boolean doWithLock() {
                AllocationAttempt attempt = new AllocationAttempt(instance, hosts, volumes, pools, nics, subnets);

                return doAllocate(request, attempt, instance);
            }
        });
    }

    protected boolean deallocateVolume(AllocationRequest request) {
        final Volume volume = objectManager.loadResource(Volume.class, request.getResourceId());
        Boolean stateCheck = AllocatorUtils.checkDeallocateState(request.getResourceId(), volume.getAllocationState(), "Volume");
        if ( stateCheck != null ) {
            return stateCheck;
        }

        releaseAllocation(volume);

        return true;
    }

    protected boolean allocateVolume(AllocationRequest request) {
        Volume volume = objectManager.loadResource(Volume.class, request.getResourceId());
        Boolean stateCheck = AllocatorUtils.checkAllocateState(request.getResourceId(), volume.getAllocationState(), "Volume");
        if ( stateCheck != null ) {
            return stateCheck;
        }

        Set<Volume> volumes = new HashSet<Volume>();
        volumes.add(volume);

        Map<Volume,Set<StoragePool>> pools = new HashMap<Volume, Set<StoragePool>>();
        Set<StoragePool> associatedPools = new HashSet<StoragePool>(allocatorDao.getAssociatedPools(volume));
        pools.put(volume, associatedPools);

        AllocationAttempt attempt = new AllocationAttempt(null, new HashSet<Host>(), volumes, pools, null, null);

        return doAllocate(request, attempt, volume);
    }

    protected boolean doAllocate(final AllocationRequest request, final AllocationAttempt attempt, Object deallocate) {
        AllocationLog log = getLog(request);
        populateConstraints(attempt, log);

        Context c = allocateTimer.time();
        try {
            return acquireLockAndAllocate(request, attempt, deallocate);
        } finally {
            c.stop();
        }
    }

    protected boolean acquireLockAndAllocate(final AllocationRequest request, final AllocationAttempt attempt, Object deallocate) {
        lockManager.lock(getAllocationLock(request, attempt), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                runAllocation(request, attempt);
            }
        });

        if ( attempt.getMatchedCandidate() == null ) {
            return false;
        }

        return true;
    }

    protected void runAllocation(AllocationRequest request, AllocationAttempt attempt) {
        logStart(attempt);

        Iterator<AllocationCandidate> iter = getCandidates(attempt);
        try {
            while ( iter.hasNext() ) {
                AllocationCandidate candidate = iter.next();
                attempt.getCandidates().add(candidate);

                String prefix = String.format("[%s][%s]", attempt.getId(), candidate.getId());
                logCandidate(prefix, attempt, candidate);

                boolean good = true;
                for ( Constraint constraint : attempt.getConstraints() ) {
                    boolean match = constraint.matches(attempt, candidate);
                    log.info("{}   checking candidate [{}] : {}", prefix, match, constraint);
                    if ( ! match ) {
                        good = false;
                    }
                }

                log.info("{}   candidates result [{}]", prefix, good);
                if ( good ) {
                    if ( candidate.getHosts().size() > 0 && request.getType() == Type.VOLUME ) {
                        throw new IllegalStateException("Attempting to allocate hosts during a volume allocation");
                    }

                    if ( recordCandidate(attempt, candidate) ) {
                        attempt.setMatchedCandidate(candidate);
                        return;
                    } else {
                        log.info("{}   can not record result", prefix);
                    }
                }
            }
        } finally {
            if ( iter != null ) {
                close(iter);
            }
        }
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
        for ( long hostId : candidate.getHosts() ) {
            log.info("{}   host [{}]", prefix, hostId);
        }
        for ( Map.Entry<Long,Set<Long>> entry : candidate.getPools().entrySet() ) {
            log.info("{}   volume [{}]", prefix, entry.getKey());
            for ( long poolId : entry.getValue() ) {
                log.info("{}     pool [{}]", prefix, poolId);
            }
        }
        for ( Map.Entry<Long, Long> entry : candidate.getSubnetIds().entrySet() ) {
            log.info("{}   nic [{}] subnet [{}]", prefix, entry.getKey(), entry.getValue());
        }
    }

    protected void logStart(AllocationAttempt request) {
        String id = request.getId();
        log.info("[{}] Attemping allocation for:", id);
        if ( request.getInstance() != null ) {
            log.info("[{}]   instance [{}]", id, request.getInstance().getId());
        }
        for ( Map.Entry<Volume, Set<StoragePool>> entry : request.getPools().entrySet() ) {
            long volumeId = entry.getKey().getId();
            log.info("[{}]   volume [{}]", id, volumeId);
            for ( StoragePool pool : entry.getValue() ) {
                log.info("[{}]     pool [{}]", id, pool.getId());
            }
        }
        log.info("[{}] constraints:", id);
        for ( Constraint constraint : request.getConstraints()) {
            log.info("[{}]   {}", id, constraint);
        }
    }

    protected void close(Iterator<AllocationCandidate> iter) {
    }

    protected abstract Iterator<AllocationCandidate> getCandidates(AllocationAttempt instanceRequest);

    protected void populateConstraints(AllocationAttempt attempt, AllocationLog log) {
        List<Constraint> constraints = attempt.getConstraints();

        for ( AllocationConstraintsProvider provider : allocationConstraintProviders ) {
            provider.appendConstraints(attempt, log, constraints);
        }
    }

    protected abstract boolean supports(AllocationRequest request);

    public LockManager getLockManager() {
        return lockManager;
    }

    @Inject
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    public AllocatorDao getAllocatorDao() {
        return allocatorDao;
    }

    @Inject
    public void setAllocatorDao(AllocatorDao allocatorDao) {
        this.allocatorDao = allocatorDao;
    }

    public ObjectProcessManager getProcessManager() {
        return processManager;
    }

    @Inject
    public void setProcessManager(ObjectProcessManager processManager) {
        this.processManager = processManager;
    }

    public List<AllocationConstraintsProvider> getAllocationConstraintProviders() {
        return allocationConstraintProviders;
    }

    @Inject
    public void setAllocationConstraintProviders(List<AllocationConstraintsProvider> allocationConstraintProviders) {
        this.allocationConstraintProviders = allocationConstraintProviders;
    }

}
