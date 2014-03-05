package io.github.ibuildthecloud.dstack.allocator.constraint;

import io.github.ibuildthecloud.dstack.allocator.dao.AllocatorDao;
import io.github.ibuildthecloud.dstack.allocator.service.AllocationAttempt;
import io.github.ibuildthecloud.dstack.allocator.service.AllocationLog;
import io.github.ibuildthecloud.dstack.core.constants.InstanceConstants;
import io.github.ibuildthecloud.dstack.core.model.Host;
import io.github.ibuildthecloud.dstack.core.model.Instance;
import io.github.ibuildthecloud.dstack.core.model.StoragePool;
import io.github.ibuildthecloud.dstack.core.model.Volume;
import io.github.ibuildthecloud.dstack.object.ObjectManager;
import io.github.ibuildthecloud.dstack.object.util.DataUtils;
import io.github.ibuildthecloud.dstack.util.type.Priority;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

public class BaseConstraintsProvider implements AllocationConstraintsProvider, Priority {

    AllocatorDao allocatorDao;
    ObjectManager objectManager;

    @Override
    public void appendConstraints(AllocationAttempt attempt, AllocationLog log, List<Constraint> constraints) {
        populateConstraints(attempt, log);
    }

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

        Instance instance = attempt.getInstance();
        if ( instance != null ) {
            Long requestedHostId = DataUtils.getField(instance.getData(), InstanceConstants.FIELD_REQUESTED_HOST_ID, Long.class);
            if ( requestedHostId != null ) {
                hostSet.addHost(requestedHostId);
            }
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

            Instance instance = objectManager.loadResource(Instance.class, volume.getInstanceId());
            if ( instance != null ) {
                for ( Host host : allocatorDao.getHosts(instance) ) {
                    for ( StoragePool pool : allocatorDao.getAssociatedPools(host) ) {
                        volumeToPoolConstraint.getStoragePools().add(pool.getId());
                    }
                }
            }

            if ( volumeToPoolConstraint.getStoragePools().size() > 0 ) {
                constraints.add(volumeToPoolConstraint);
            }
        }
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

    @Override
    public int getPriority() {
        return Priority.PRE;
    }

}
