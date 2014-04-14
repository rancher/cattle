package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.dao.AllocatorDao;
import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationCandidate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HostVnetMatchConstraint implements Constraint {

    AllocatorDao allocatorDao;
    long nicId;

    Map<Long,Set<Long>> vnetIdHostIds = new HashMap<Long, Set<Long>>();

    public HostVnetMatchConstraint(long nicId, AllocatorDao allocatorDao) {
        super();
        this.nicId = nicId;
        this.allocatorDao = allocatorDao;
    }

    @Override
    public boolean matches(AllocationAttempt attempt, AllocationCandidate candidate) {
        Long subnetId = candidate.getSubnetIds().get(nicId);
        Set<Long> hostIds = candidate.getHosts();

        if ( subnetId != null && hostIds != null ) {
            for ( long hostId : hostIds ) {
                Set<Long> validHostIds = vnetIdHostIds.get(subnetId);
                if ( validHostIds == null ) {
                    validHostIds = new HashSet<Long>();
                    validHostIds.addAll(allocatorDao.getHostsForSubnet(subnetId));

                    vnetIdHostIds.put(subnetId, validHostIds);
                }

                if ( validHostIds.size() > 0 && ! validHostIds.contains(hostId) ) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return String.format("nic [%s] subnet's vnet matches host", nicId);
    }

}