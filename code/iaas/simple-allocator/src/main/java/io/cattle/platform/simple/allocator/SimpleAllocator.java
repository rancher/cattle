package io.cattle.platform.simple.allocator;

import io.cattle.platform.allocator.constraint.Constraint;
import io.cattle.platform.allocator.constraint.ValidHostsConstraint;
import io.cattle.platform.allocator.service.AbstractAllocator;
import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.allocator.service.AllocationRequest;
import io.cattle.platform.allocator.service.Allocator;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.simple.allocator.dao.QueryOptions;
import io.cattle.platform.simple.allocator.dao.SimpleAllocatorDao;
import io.cattle.platform.simple.allocator.dao.impl.AllocationCandidateIterator;
import io.cattle.platform.simple.allocator.network.NetworkAllocationCandidates;
import io.cattle.platform.util.type.Named;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

public class SimpleAllocator extends AbstractAllocator implements Allocator, Named {

    String name = getClass().getSimpleName();

    @Inject
    SimpleAllocatorDao simpleAllocatorDao;

    @Override
    protected LockDefinition getAllocationLock(AllocationRequest request, AllocationAttempt attempt) {
        if (attempt != null) {
            return new AccountAllocatorLock(attempt.getAccountId());
        }

        return new SimpleAllocatorLock();
    }

    @Override
    protected Iterator<AllocationCandidate> getCandidates(AllocationAttempt request) {
        List<Long> volumeIds = new ArrayList<Long>(request.getVolumeIds());

        QueryOptions options = new QueryOptions();

        options.setAccountId(request.getAccountId());

        for (Constraint constraint : request.getConstraints()) {
            if (constraint instanceof ValidHostsConstraint) {
                options.getHosts().addAll(((ValidHostsConstraint) constraint).getHosts());
            }
        }

        if (request.getInstance() == null) {
            return simpleAllocatorDao.iteratorPools(volumeIds, options);
        } else {
            return simpleAllocatorDao.iteratorHosts(volumeIds, options, getCallback(request));
        }
    }

    protected AllocationCandidateCallback getCallback(AllocationAttempt request) {
        if (request.getInstance() == null) {
            return null;
        }

        return new NetworkAllocationCandidates(objectManager, request);
    }

    @Override
    protected void close(Iterator<AllocationCandidate> iter) {
        super.close(iter);

        if (iter instanceof AllocationCandidateIterator) {
            ((AllocationCandidateIterator) iter).close();
        }
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
