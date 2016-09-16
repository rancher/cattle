package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationCandidate;

import java.util.HashSet;
import java.util.Set;

public class ValidHostsConstraint extends HardConstraint implements Constraint {

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
    public boolean matches(AllocationCandidate candidate) {
        return candidate.getHost() == null ? true : hosts.contains(candidate.getHost());
    }

}
