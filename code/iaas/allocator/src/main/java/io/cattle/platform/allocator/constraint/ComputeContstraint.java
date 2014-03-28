package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.core.model.Host;

public class ComputeContstraint implements Constraint {

    long computeFree;

    public ComputeContstraint(long computeFree) {
        this.computeFree = computeFree;
    }

    @Override
    public boolean matches(AllocationAttempt attempt, AllocationCandidate candidate) {
        for ( long id : candidate.getHosts() ) {
            Host host = candidate.loadResource(Host.class, id);
            Long free = host.getComputeFree();

            if ( free != null && free.longValue() < computeFree ) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return String.format("host needs %s compute free", computeFree);
    }

    public long getComputeFree() {
        return computeFree;
    }

    public void setComputeFree(long computeFree) {
        this.computeFree = computeFree;
    }

}
