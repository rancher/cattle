package io.github.ibuildthecloud.dstack.allocator.service;

import io.github.ibuildthecloud.dstack.allocator.dao.AllocatorDao;
import io.github.ibuildthecloud.dstack.allocator.exception.UnsupportedAllocation;
import io.github.ibuildthecloud.dstack.allocator.lock.AllocateResourceLock;
import io.github.ibuildthecloud.dstack.allocator.lock.AllocateVolumesResourceLock;
import io.github.ibuildthecloud.dstack.allocator.service.AllocationRequest.Type;
import io.github.ibuildthecloud.dstack.core.model.Host;
import io.github.ibuildthecloud.dstack.core.model.Instance;
import io.github.ibuildthecloud.dstack.core.model.StoragePool;
import io.github.ibuildthecloud.dstack.core.model.Volume;
import io.github.ibuildthecloud.dstack.lock.LockCallback;
import io.github.ibuildthecloud.dstack.lock.LockCallbackNoReturn;
import io.github.ibuildthecloud.dstack.lock.LockManager;
import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;
import io.github.ibuildthecloud.dstack.object.ObjectManager;
import io.github.ibuildthecloud.dstack.object.process.ObjectProcessManager;
import io.github.ibuildthecloud.dstack.object.process.StandardProcess;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import o.github.ibuildthecloud.dstack.allocator.util.AllocatorUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractAllocator implements Allocator {

    private static final Logger log = LoggerFactory.getLogger(AbstractAllocator.class);

    AllocatorDao allocatorDao;
    LockManager lockManager;
    ObjectManager objectManager;
    ObjectProcessManager processManager;
    boolean unallocateOnFailure;

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
                    switch (request.getType()) {
                    case INSTANCE:
                        return deallocateInstance(request);
                    case VOLUME:
                        return deallocateVolume(request);
                    }

                    return false;
                }
            });
        } catch( UnsupportedAllocation e ) {
            log.info("Unsupported allocation for [{}] : {}", this, e.getMessage());
            return false;
        }
    }

    protected boolean deallocateInstance(final AllocationRequest request) {
        final Instance instance = objectManager.loadResource(Instance.class, request.getResourceId());
        boolean stateCheck = AllocatorUtils.checkDeallocateState(request.getResourceId(), instance.getAllocationState(), "Instance");
        if ( ! stateCheck ) {
            return true;
        }

        releaseAllocation(instance);

        return true;
    }

    protected void releaseAllocation(Instance instance) {
        allocatorDao.releaseAllocation(instance);
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

        return lockManager.lock(new AllocateVolumesResourceLock(volumes), new LockCallback<Boolean>() {
            @Override
            public Boolean doWithLock() {
                AllocationAttempt attempt = new AllocationAttempt(instance, hosts, volumes, pools);

                return doAllocate(request, attempt, instance);
            }
        });
    }

    protected boolean deallocateVolume(AllocationRequest request) {
        throw new UnsupportedOperationException();
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
        pools.put(volume, new HashSet<StoragePool>(allocatorDao.getAssociatedPools(volume)));

        AllocationAttempt attempt = new AllocationAttempt(null, new HashSet<Host>(), volumes, pools);

        return doAllocate(request, attempt, volume);
    }

    protected boolean doAllocate(final AllocationRequest request, final AllocationAttempt attempt, Object deallocate) {
        AllocationLog log = getLog(request);
        populateConstraints(attempt, log);

        lockManager.lock(getAllocationLock(request, attempt), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                runAllocation(request, attempt);
            }
        });

        if ( attempt.getMatchedCandidate() == null ) {
            if ( unallocateOnFailure ) {
                processManager.scheduleStandardProcess(StandardProcess.DEALLOCATE, deallocate, null);
            }
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
        addLogConstraints(attempt, log);
        addComputeConstraints(attempt, constraints);
        addStorageConstraints(attempt, constraints);
    }

    protected void addLogConstraints(AllocationAttempt attempt, AllocationLog log) {
    }

    protected void addComputeConstraints(AllocationAttempt attempt, List<Constraint> constraints) {
        ValidHostsConstraint hostSet = new ValidHostsConstraint();
        for ( Host host : attempt.getHosts() ) {
            hostSet.addHost(host.getId());
        }

        if ( hostSet.getHosts().size() > 0 ) {
            constraints.add(hostSet);
        }
    }

    protected void addStorageConstraints(AllocationAttempt attempt, List<Constraint> constraints) {
        for ( Map.Entry<Volume, Set<StoragePool>> entry : attempt.getPools().entrySet() ) {
            Volume volume = entry.getKey();
            VolumeValidStoragePoolConstraint volumeToPoolConstraint = new VolumeValidStoragePoolConstraint(volume);

            for ( StoragePool pool : entry.getValue() ) {
                volumeToPoolConstraint.getStoragePools().add(pool.getId());
                ValidHostsConstraint hostSet = new ValidHostsConstraint();
                for ( Host host : allocatorDao.getHosts(pool) ) {
                    hostSet.addHost(host.getId());
                }
                constraints.add(hostSet);
            }

            if ( volumeToPoolConstraint.getStoragePools().size() > 0 ) {
                constraints.add(volumeToPoolConstraint);
            }
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

    public boolean isUnallocateOnFailure() {
        return unallocateOnFailure;
    }

    public void setUnallocateOnFailure(boolean unallocateOnFailure) {
        this.unallocateOnFailure = unallocateOnFailure;
    }

    public ObjectProcessManager getProcessManager() {
        return processManager;
    }

    @Inject
    public void setProcessManager(ObjectProcessManager processManager) {
        this.processManager = processManager;
    }

}
