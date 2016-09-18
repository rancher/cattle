package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.dao.AllocatorDao;
import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.object.ObjectManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HostVnetMatchConstraint extends HardConstraint implements Constraint {

    ObjectManager objectManager;
    AllocatorDao allocatorDao;
    long nicId;
    Long vnetId;

    Map<Long, Set<Long>> subnetIdHostIds = new HashMap<Long, Set<Long>>();
    Map<Long, Set<Long>> subnetIdPhysicalHostIds = new HashMap<Long, Set<Long>>();

    public HostVnetMatchConstraint(long nicId, Long vnetId, ObjectManager objectManager, AllocatorDao allocatorDao) {
        super();
        this.nicId = nicId;
        this.vnetId = vnetId;
        this.allocatorDao = allocatorDao;
        this.objectManager = objectManager;
    }

    @Override
    public boolean matches(AllocationCandidate candidate) {
        Long subnetId = candidate.getSubnetIds().get(nicId);
        Long hostId = candidate.getHost();

        if (subnetId != null && hostId != null) {
            Set<Long> validHostIds = subnetIdHostIds.get(subnetId);
            if (validHostIds == null) {
                validHostIds = new HashSet<Long>();
                validHostIds.addAll(allocatorDao.getHostsForSubnet(subnetId, vnetId));

                subnetIdHostIds.put(subnetId, validHostIds);
            }

            if (validHostIds.contains(hostId)) {
                return true;
            }

            Host host = objectManager.loadResource(Host.class, hostId);

            if (host.getPhysicalHostId() == null) {
                if (validHostIds.size() != 0) {
                    return false;
                }
            } else {
                Set<Long> validPhysicalHosts = subnetIdPhysicalHostIds.get(subnetId);
                if (validPhysicalHosts == null) {
                    validPhysicalHosts = new HashSet<Long>();
                    validPhysicalHosts.addAll(allocatorDao.getPhysicalHostsForSubnet(subnetId, vnetId));

                    subnetIdPhysicalHostIds.put(subnetId, validPhysicalHosts);
                }

                if (validPhysicalHosts.size() > 0 && !validPhysicalHosts.contains(host.getPhysicalHostId())) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return String.format("nic [%s] subnet's matches host and vnet [%s]", nicId, vnetId);
    }

}