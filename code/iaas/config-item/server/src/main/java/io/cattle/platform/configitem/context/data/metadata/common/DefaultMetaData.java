package io.cattle.platform.configitem.context.data.metadata.common;

public class DefaultMetaData {
    public static class Self {
        HostMetaData host;
        public Self(HostMetaData host) {
            super();
            this.host = host;
        }

        public HostMetaData getHost() {
            return host;
        }

        public void setHost(HostMetaData host) {
            this.host = host;
        }
    }

    String version;
    Self self;
    // helper field needed by metadata service to process object
    String metadata_kind;

    public DefaultMetaData(String version, HostMetaData host) {
        super();
        this.version = version;
        this.self = new Self(host);
        this.metadata_kind = "defaultData";
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Self getSelf() {
        return self;
    }

    public void setSelf(Self self) {
        this.self = self;
    }

    public String getMetadata_kind() {
        return metadata_kind;
    }

    public void setMetadata_kind(String metadata_kind) {
        this.metadata_kind = metadata_kind;
    }
}
