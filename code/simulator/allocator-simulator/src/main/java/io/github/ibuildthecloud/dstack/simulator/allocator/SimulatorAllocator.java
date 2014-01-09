package io.github.ibuildthecloud.dstack.simulator.allocator;

import io.github.ibuildthecloud.dstack.allocator.service.AbstractAllocator;
import io.github.ibuildthecloud.dstack.allocator.service.AllocationAttempt;
import io.github.ibuildthecloud.dstack.allocator.service.AllocationCandidate;
import io.github.ibuildthecloud.dstack.allocator.service.AllocationRequest;
import io.github.ibuildthecloud.dstack.allocator.service.Allocator;
import io.github.ibuildthecloud.dstack.core.model.Volume;
import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;
import io.github.ibuildthecloud.dstack.simulator.allocator.dao.SimulatorAllocatorDao;
import io.github.ibuildthecloud.dstack.simulator.allocator.dao.impl.AllocationCandidateIterator;
import io.github.ibuildthecloud.dstack.util.type.NamedUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

public class SimulatorAllocator extends AbstractAllocator implements Allocator {

    SimulatorAllocatorDao simulatorAllocatorDao;

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
            return simulatorAllocatorDao.iteratorPools(volumeIds);
        } else {
            return simulatorAllocatorDao.iteratorHosts(volumeIds);
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
            return simulatorAllocatorDao.isSimulatedInstance(request.getResourceId());
        case VOLUME:
            return simulatorAllocatorDao.isSimulatedVolume(request.getResourceId());
        }

        return false;
    }

    public SimulatorAllocatorDao getSimulatorAllocatorDao() {
        return simulatorAllocatorDao;
    }

    @Inject
    public void setSimulatorAllocatorDao(SimulatorAllocatorDao simulatorAllocatorDao) {
        this.simulatorAllocatorDao = simulatorAllocatorDao;
    }

    @Override
    public String toString() {
        return NamedUtils.getName(this);
    }

}
