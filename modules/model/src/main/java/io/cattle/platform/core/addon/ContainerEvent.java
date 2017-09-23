package io.cattle.platform.core.addon;

import io.cattle.platform.core.constants.ContainerEventConstants;
import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.Map;

@Type(list = false)
public class ContainerEvent {

    Map<String, Object> dockerInspect;
    String externalId;
    String externalStatus;
    String reportedHostUuid;
    Long hostId;
    Long clusterId;

    public ContainerEvent() {
    }

    public ContainerEvent(String status, long clusterId, long hostId, String externaId) {
        this.externalStatus = status;
        this.clusterId = clusterId;
        this.externalId = externaId;
        this.hostId = hostId;
    }

    public ContainerEvent(long clusterId, long hostId, String externaId, Map<String, Object> inspect) {
        this.dockerInspect = inspect;
        this.externalId = externaId;
        this.externalStatus = ContainerEventConstants.EVENT_START;
        this.clusterId = clusterId;
        this.hostId = hostId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getExternalStatus() {
        return externalStatus;
    }

    public void setExternalStatus(String externalStatus) {
        this.externalStatus = externalStatus;
    }

    public String getReportedHostUuid() {
        return reportedHostUuid;
    }

    public void setReportedHostUuid(String reportedHostUuid) {
        this.reportedHostUuid = reportedHostUuid;
    }

    @Field(typeString = "reference[host]")
    public Long getHostId() {
        return hostId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    public Map<String, Object> getDockerInspect() {
        return dockerInspect;
    }

    public void setDockerInspect(Map<String, Object> dockerInspect) {
        this.dockerInspect = dockerInspect;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public void setClusterId(Long clusterId) {
        this.clusterId = clusterId;
    }

}
