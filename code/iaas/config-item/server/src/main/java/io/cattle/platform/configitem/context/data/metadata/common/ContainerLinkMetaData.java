package io.cattle.platform.configitem.context.data.metadata.common;

public class ContainerLinkMetaData {
    String container_uuid;
    String key;
    String value;
    String metadata_kind;

    public ContainerLinkMetaData(String container_uuid, String target_container_uuid, String link_name) {
        super();
        this.container_uuid = container_uuid;
        this.value = target_container_uuid;
        this.key = link_name;
        this.metadata_kind = "containerLink";
    }

    public String getContainer_uuid() {
        return container_uuid;
    }

    public void setContainer_uuid(String container_uuid) {
        this.container_uuid = container_uuid;
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
