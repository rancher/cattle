package io.github.ibuildthecloud.dstack.simple.allocator;

import io.github.ibuildthecloud.dstack.allocator.service.AbstractAllocator;
import io.github.ibuildthecloud.dstack.allocator.service.AllocationAttempt;
import io.github.ibuildthecloud.dstack.allocator.service.AllocationCandidate;
import io.github.ibuildthecloud.dstack.allocator.service.AllocationRequest;
import io.github.ibuildthecloud.dstack.allocator.service.Allocator;
import io.github.ibuildthecloud.dstack.core.model.Volume;
import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;
import io.github.ibuildthecloud.dstack.simple.allocator.dao.SimpleAllocatorDao;
import io.github.ibuildthecloud.dstack.simple.allocator.dao.impl.AllocationCandidateIterator;
import io.github.ibuildthecloud.dstack.util.type.Named;
import io.github.ibuildthecloud.dstack.util.type.NamedUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

public class SimpleAllocator extends AbstractAllocator implements Allocator, Named {

    String name = getClass().getSimpleName();
    String kind;
    SimpleAllocatorDao simpleAllocatorDao;

    @Override
    protected LockDefinition getAllocationLock(AllocationRequest request, AllocationAttempt attempt) {
        return null;
    }

    @Override
    protected Iterator<AllocationCandidate> getCandidates(AllocationAttempt request) {
        List<Long> volumeIds = new ArrayList<Long>(request.getVolumes().size());
        for ( Volume v : request.getVolumes() ) {
            volumeIds.add(v.getId());
        }

        if ( request.getInstance() == null ) {
            return simpleAllocatorDao.iteratorPools(volumeIds, kind);
        } else {
            return simpleAllocatorDao.iteratorHosts(volumeIds, kind);
        }
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
        switch(request.getType()) {
        case INSTANCE:
            return simpleAllocatorDao.isInstance(request.getResourceId(), kind);
        case VOLUME:
            return simpleAllocatorDao.isVolume(request.getResourceId(), kind);
        }

        return false;
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

    public String getKind() {
        return kind;
    }

    @Inject
    public void setKind(String kind) {
        this.kind = kind;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
