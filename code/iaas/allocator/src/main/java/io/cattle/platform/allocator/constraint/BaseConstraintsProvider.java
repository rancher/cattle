package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.dao.AllocatorDao;
import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationLog;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.StoragePoolDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.core.model.Vnet;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.Priority;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

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
        addNetworkConstraints(attempt, constraints);
        addStorageConstraints(attempt, constraints);
    }

    protected void addLogConstraints(AllocationAttempt attempt, AllocationLog log) {
    }

    protected void addNetworkConstraints(AllocationAttempt attempt, List<Constraint> constraints) {
        for (Nic nic : attempt.getNics()) {
            Long vnetId = nic.getVnetId();
            Long subnetId = nic.getSubnetId();

            ValidSubnetsConstraint constraint = new ValidSubnetsConstraint(nic.getId());
            if (subnetId != null) {
                constraint.addSubnet(subnetId);
            }

            if (vnetId != null) {
                Vnet vnet = objectManager.loadResource(Vnet.class, vnetId);
                if (vnet != null) {
                    for (Subnet subnet : objectManager.mappedChildren(vnet, Subnet.class)) {
                        constraint.addSubnet(subnet.getId());
                    }
                }
            }

            if (constraint.getSubnets().size() > 0) {
                constraints.add(constraint);
            }
        }
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
        for (Map.Entry<Volume, Set<StoragePool>> entry : attempt.getPools().entrySet()) {
            Volume volume = entry.getKey();
            boolean alreadyMappedToPool = entry.getValue().size() > 0;
            Set<Long> storagePoolIds = new HashSet<>();
            for (StoragePool pool : entry.getValue()) {
                storagePoolIds.add(pool.getId());
                storagePoolToHostConstraint(constraints, pool);
            }

            if (!alreadyMappedToPool) {
                String driver = DataAccessor.fieldString(volume, VolumeConstants.FIELD_VOLUME_DRIVER);
                boolean restrictToUnmanagedPool = true;
                if (StringUtils.isNotEmpty(driver) && !VolumeConstants.LOCAL_DRIVER.equals(driver)) {
                    StoragePool pool = storagePoolDao.findStoragePoolByDriverName(volume.getAccountId(), driver);
                    if (pool != null) {
                        Set<Long> poolIds = new HashSet<>();
                        poolIds.add(pool.getId());
                        constraints.add(new VolumeValidStoragePoolConstraint(volume, false, poolIds));
                        storagePoolToHostConstraint(constraints, pool);
                        restrictToUnmanagedPool = false;
                    }
                }

                if (restrictToUnmanagedPool) {
                    constraints.add(new UnmanagedStoragePoolKindConstraint(volume));
                }

                Instance instance = objectManager.loadResource(Instance.class, volume.getInstanceId());
                if (instance != null) {
                    Host host = allocatorDao.getHost(instance);
                    if (host != null) {
                        for (StoragePool pool : allocatorDao.getAssociatedUnmanagedPools(host)) {
                            storagePoolIds.add(pool.getId());
                        }
                    }
                }
            }

            if (storagePoolIds.size() > 0) {
                constraints.add(new VolumeValidStoragePoolConstraint(volume, alreadyMappedToPool, storagePoolIds));
            }

            if (volume.getImageId() != null) {
                constraints.add(new UnmanagedStoragePoolKindConstraint(volume));
            }
        }

        if (attempt.isInstanceAllocation()) {
            for (Instance instance : attempt.getInstances()) {
                String driver = DataAccessor.fieldString(instance, InstanceConstants.FIELD_VOLUME_DRIVER);
                if (StringUtils.isNotEmpty(driver) && !VolumeConstants.LOCAL_DRIVER.equals(driver)) {
                    StoragePool pool = storagePoolDao.findStoragePoolByDriverName(instance.getAccountId(), driver);
                    if (pool != null) {
                        storagePoolToHostConstraint(constraints, pool);
                    }
                }
            }
        }
    }

    void storagePoolToHostConstraint(List<Constraint> constraints, StoragePool pool) {
        ValidHostsConstraint hostSet = new ValidHostsConstraint();
        for (Host host : allocatorDao.getHosts(pool)) {
            hostSet.addHost(host.getId());
        }
        constraints.add(hostSet);
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
