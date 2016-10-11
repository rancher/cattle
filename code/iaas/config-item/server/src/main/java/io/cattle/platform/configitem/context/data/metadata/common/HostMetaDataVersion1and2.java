package io.cattle.platform.configitem.context.data.metadata.common;



public class HostMetaDataVersion1and2 extends HostMetaData {
    public HostMetaDataVersion1and2(HostMetaData data) {
        super(data);
    }

    public Long getHostId() {
        return hostId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

}
