package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.exception.FailedToAllocate;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

public class CollocationChecker {

    @Inject
    GenericMapDao mapDao;

    /**
     * Returns a map of host ids to instance ids that should be used to check candidate host ids. The instance ids are provided so that constraints can use them
     * to provide meaningful output when failing a candidate.
     * 
     * The map will only ever have a length of 0 or 1. We need to return the host id and instance ids and a map is the most convenient structure for doing so.
     * Instead of returning a map with a length greater than 1, a FailedToAllocate exception will be raised because that means collocated instances are on
     * different hosts.
     * 
     * @param collocatedInstances
     * @param coscheduledInstances
     * @return
     * @throws FailedToAllocate
     *             if a collocated instance is not yet allocated and is not part of the coscheduledInstances or if the collocated instances are not on the same
     *             host already.
     */
    public Map<Long, Set<Long>> checkAndGetCollocatedInstanceHosts(Set<Long> collocatedInstances, Collection<Instance> coscheduledInstances) {
        for (Instance instance : coscheduledInstances) {
            collocatedInstances.remove(instance.getId());
        }

        Map<Long, Set<Long>> hostsOfCollocatedInstances = new HashMap<>();
        for (Long instanceId : collocatedInstances) {
            List<? extends InstanceHostMap> maps = mapDao.findNonRemoved(InstanceHostMap.class, Instance.class, instanceId);
            if (maps.size() > 0) {
                Long hostId = maps.get(0).getHostId();
                if (hostId == null) {
                    throw new FailedToAllocate(String.format("Dependent instance not allocated yet: %s.", instanceId));
                }
                if (!hostsOfCollocatedInstances.containsKey(hostId)) {
                    hostsOfCollocatedInstances.put(hostId, new HashSet<Long>());
                }
                hostsOfCollocatedInstances.get(hostId).add(instanceId);
            } else {
                throw new FailedToAllocate(String.format("Dependent instance not allocated yet: %s.", instanceId));
            }
        }

        if (hostsOfCollocatedInstances.keySet().size() > 1) {
            throw new FailedToAllocate(String.format(
                    "Dependent instances are allocated to different hosts. Hosts and the dependent instances allocated to them: %s.",
                    hostsOfCollocatedInstances));
        }

        return hostsOfCollocatedInstances;
    }
}
