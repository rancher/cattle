package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.core.model.Host;

public class HostKindConstraint implements Constraint, KindConstraint {

    String kind;

    public HostKindConstraint(String kind) {
        super();
        this.kind = kind;
    }

    @Override
    public boolean matches(AllocationAttempt attempt, AllocationCandidate candidate) {
        for (Long id : candidate.getHosts()) {
            Host host = candidate.loadResource(Host.class, id);

            if (!kind.equals(host.getKind())) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return String.format("host.kind must be %s", kind);
    }

    @Override
    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

}
