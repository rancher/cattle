package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationCandidate;

public class SingleHostVolumeInstanceConstraint implements Constraint {

    Long hostId;
    Long volumeId;
    boolean hard;

    public SingleHostVolumeInstanceConstraint(Long hostId, Long volumeId, boolean hard) {
        super();
        this.hostId = hostId;
        this.volumeId = volumeId;
        this.hard = hard;
    }

    @Override
    public String toString() {
        return String.format("Volume %s can only be used on host %s", volumeId, hostId);
    }

    @Override
    public boolean matches(AllocationAttempt attempt, AllocationCandidate candidate) {
        return candidate.getHosts().size() == 1 && candidate.getHosts().contains(hostId);
    }

    @Override
    public final boolean isHardConstraint() {
        return hard;
    }
}
