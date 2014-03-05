package io.github.ibuildthecloud.dstack.allocator.constraint;

import io.github.ibuildthecloud.dstack.allocator.service.AllocationAttempt;
import io.github.ibuildthecloud.dstack.allocator.service.AllocationCandidate;

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
        return String.format("must be host(s) %s", hosts);
    }

    @Override
    public boolean matches(AllocationAttempt attempt, AllocationCandidate candidate) {
        return hosts.containsAll(candidate.getHosts());
    }

}
