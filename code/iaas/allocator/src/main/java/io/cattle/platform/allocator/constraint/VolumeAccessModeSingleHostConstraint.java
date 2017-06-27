package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

public class VolumeAccessModeSingleHostConstraint implements Constraint {
    Long hostId;
    Long volumeId;
    String volumeName;
    String hostName;
    boolean hard;
    IdFormatter idFmt;

    public VolumeAccessModeSingleHostConstraint(Long hostId, Long volumeId, String volumeName, String hostName, boolean hard, IdFormatter idFmt) {
        super();
        this.hostId = hostId;
        this.volumeId = volumeId;
        this.volumeName = volumeName;
        this.hostName = hostName;
        this.hard = hard;
        this.idFmt = idFmt;
    }

    @Override
    public String toString() {
        String hID = idFmt.formatId(HostConstants.TYPE, hostId).toString();
        String vID = idFmt.formatId(VolumeConstants.TYPE, volumeId).toString();
        return String.format("Volume %s(id: %s) can only be used on host %s(id: %s)", volumeName, vID, hostName, hID);
    }

    @Override
    public boolean matches(AllocationCandidate candidate) {
        return hostId.equals(candidate.getHost());
    }

    @Override
    public final boolean isHardConstraint() {
        return hard;
    }
}
