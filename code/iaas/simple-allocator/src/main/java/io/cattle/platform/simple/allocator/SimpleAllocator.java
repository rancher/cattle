package io.cattle.platform.simple.allocator;

import io.cattle.platform.allocator.constraint.ComputeContstraint;
import io.cattle.platform.allocator.constraint.Constraint;
import io.cattle.platform.allocator.constraint.KindConstraint;
import io.cattle.platform.allocator.constraint.ValidHostsConstraint;
import io.cattle.platform.allocator.service.AbstractAllocator;
import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.allocator.service.AllocationRequest;
import io.cattle.platform.allocator.service.Allocator;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.simple.allocator.dao.QueryOptions;
import io.cattle.platform.simple.allocator.dao.SimpleAllocatorDao;
import io.cattle.platform.simple.allocator.dao.impl.AllocationCandidateIterator;
import io.cattle.platform.simple.allocator.network.NetworkAllocationCandidates;
import io.cattle.platform.util.type.Named;
import io.cattle.platform.util.type.NamedUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

public class SimpleAllocator extends AbstractAllocator implements Allocator, Named {

    String name = getClass().getSimpleName();
    SimpleAllocatorDao simpleAllocatorDao;

    @Override
    protected synchronized boolean acquireLockAndAllocate(AllocationRequest request, AllocationAttempt attempt, Object deallocate) {
        /* Overriding just to add synchronized */
        return super.acquireLockAndAllocate(request, attempt, deallocate);
    }

    @Override
    protected synchronized boolean acquireLockAndDeallocate(AllocationRequest request) {
        /* Overriding just to add synchronized */
        return super.acquireLockAndDeallocate(request);
    }

    @Override
    protected LockDefinition getAllocationLock(AllocationRequest request, AllocationAttempt attempt) {
        return new SimpleAllocatorLock();
    }

    @Override
    protected Iterator<AllocationCandidate> getCandidates(AllocationAttempt request) {
        List<Long> volumeIds = new ArrayList<Long>(request.getVolumes().size());
        for ( Volume v : request.getVolumes() ) {
            volumeIds.add(v.getId());
        }

        QueryOptions options = new QueryOptions();

        for ( Constraint constraint : request.getConstraints() ) {
            if ( constraint instanceof KindConstraint ) {
                options.setKind(((KindConstraint)constraint).getKind());
            }

            if ( constraint instanceof ComputeContstraint ) {
                options.setCompute(((ComputeContstraint)constraint).getComputeFree());
            }

            if ( constraint instanceof ValidHostsConstraint ) {
                options.getHosts().addAll(((ValidHostsConstraint)constraint).getHosts());
            }
        }

        if ( request.getInstance() == null ) {
            return simpleAllocatorDao.iteratorPools(volumeIds, options);
        } else {
            return simpleAllocatorDao.iteratorHosts(volumeIds, options, getCallback(request));
        }
    }

    protected AllocationCandidateCallback getCallback(AllocationAttempt request) {
        if ( request.getInstance() == null ) {
            return null;
        }

        return new NetworkAllocationCandidates(getObjectManager(), request);
    }


    @Override
    protected void close(Iterator<AllocationCandidate> iter) {
        super.close(iter);

        if ( iter instanceof AllocationCandidateIterator ) {
            ((AllocationCandidateIterator)iter).close();
        }
    }

    @Override
    protected boolean supports(AllocationRequest request) {
        return true;
    }

    public SimpleAllocatorDao getSimulatorAllocatorDao() {
        return simpleAllocatorDao;
    }

    @Inject
    public void setSimulatorAllocatorDao(SimpleAllocatorDao simulatorAllocatorDao) {
        this.simpleAllocatorDao = simulatorAllocatorDao;
    }

    @Override
    public String toString() {
        return NamedUtils.getName(this);
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
