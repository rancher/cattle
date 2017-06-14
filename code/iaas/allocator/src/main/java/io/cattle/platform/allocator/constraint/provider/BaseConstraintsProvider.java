package io.cattle.platform.allocator.constraint.provider;

import io.cattle.platform.allocator.constraint.Constraint;
import io.cattle.platform.allocator.constraint.ValidHostsConstraint;
import io.cattle.platform.allocator.dao.AllocatorDao;
import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationLog;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.StoragePoolDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.util.type.Priority;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

public class BaseConstraintsProvider implements AllocationConstraintsProvider, Priority {

    @Inject
    AllocatorDao allocatorDao;
    @Inject
    ObjectManager objectManager;
    @Inject
    GenericMapDao mapDao;
    @Inject
    StoragePoolDao storagePoolDao;

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
        if (attempt.getHostId() != null) {
            hostSet.addHost(attempt.getHostId());
        }

        if (attempt.getRequestedHostId() != null) {
            hostSet.addHost(attempt.getRequestedHostId());
        }

        if (hostSet.getHosts().size() > 0) {
            constraints.add(hostSet);
        }
    }

    protected void addStorageConstraints(AllocationAttempt attempt, List<Constraint> constraints) {
        for (Volume volume : attempt.getVolumes()) {
            Long hostId = allocatorDao.getHostAffinityForVolume(volume);
            if (hostId != null) {
                constraints.add(new ValidHostsConstraint(hostId));
            }
        }
    }

    void storagePoolToHostConstraint(List<Constraint> constraints, Collection<? extends StoragePool> pools) {
        ValidHostsConstraint hostSet = new ValidHostsConstraint();
        for (Host host : allocatorDao.getHosts(pools)) {
            hostSet.addHost(host.getId());
        }
        constraints.add(hostSet);
    }

    void storagePoolToHostConstraint(List<Constraint> constraints, StoragePool pool) {
        Set<StoragePool> p = new HashSet<>();
        p.add(pool);
        storagePoolToHostConstraint(constraints, p);
    }

    @Override
    public int getPriority() {
        return Priority.PRE;
    }

    @Override
    public boolean isCritical() {
        return true;
    }
}
