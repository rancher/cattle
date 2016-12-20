package io.cattle.platform.configitem.context.data.metadata.common;

public class ServiceLinkMetaData {
    String service_uuid;
    String key;
    String value;
    // helper field needed by metadata service to process object
    String metadata_kind;

    public ServiceLinkMetaData(String service_uuid, String consumed_service_name,
            String consumed_stack_name, String alias) {
        super();
        this.service_uuid = service_uuid;
        this.key = consumed_stack_name + "/" + consumed_service_name;
        this.value = alias;
        this.metadata_kind = "serviceLink";
    }

    public String getService_uuid() {
        return service_uuid;
    }

    public void setService_uuid(String service_uuid) {
        this.service_uuid = service_uuid;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getMetadata_kind() {
        return metadata_kind;
    }

    public void setMetadata_kind(String metadata_kind) {
        this.metadata_kind = metadata_kind;
    }
}
