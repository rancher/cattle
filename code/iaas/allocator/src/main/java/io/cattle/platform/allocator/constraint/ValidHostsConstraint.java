package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationCandidate;

import java.util.HashSet;
import java.util.Set;

public class ValidHostsConstraint implements Constraint {

    Set<Long> hosts = new HashSet<Long>();

    public Set<Long> getHosts() {
        return hosts;
    }

    public void addHost(long hostId) {
        hosts.add(hostId);
    }

    @Override
    public String toString() {
        return String.format("valid host(s) %s", hosts);
    }

    @Override
    public boolean matches(AllocationAttempt attempt, AllocationCandidate candidate) {
        return hosts.containsAll(candidate.getHosts());
    }

}
