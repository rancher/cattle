package io.cattle.platform.configitem.context.data.metadata.common;

public class ServiceContainerLinkMetaData {
    String service_uuid;
    String service_name;
    String container_uuid;
    // helper field needed by metadata service to process object
    String metadata_kind;

    public ServiceContainerLinkMetaData(String service_uuid, String service_name, String container_uuid) {
        super();
        this.service_uuid = service_uuid;
        this.service_name = service_name;
        this.container_uuid = container_uuid;
        this.metadata_kind = "serviceContainerLink";
    }

    public String getService_uuid() {
        return service_uuid;
    }

    public void setService_uuid(String service_uuid) {
        this.service_uuid = service_uuid;
    }

    public String getService_name() {
        return service_name;
    }

    public void setService_name(String service_name) {
        this.service_name = service_name;
    }

    public String getContainer_uuid() {
        return container_uuid;
    }

    public void setContainer_uuid(String container_uuid) {
        this.container_uuid = container_uuid;
    }

    public String getMetadata_kind() {
        return metadata_kind;
    }

    public void setMetadata_kind(String metadata_kind) {
        this.metadata_kind = metadata_kind;
    }


}
