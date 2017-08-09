package io.cattle.platform.configitem.context.data.metadata.common;

public class EnvironmentMetaData {
    String name;
    String uuid;
    String region_name;
    // helper field needed by metadata service to process object
    String metadata_kind;

    public EnvironmentMetaData(String name, String uuid, String region) {
        super();
        this.name = name;
        this.uuid = uuid;
        this.region_name = region;
        metadata_kind = "environment";
    }

    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }


    public String getUuid() {
        return uuid;
    }


    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getRegion_name() {
        return region_name;
    }

    public void setRegion_name(String region_name) {
        this.region_name = region_name;
    }


    public String getMetadata_kind() {
        return metadata_kind;
    }

    public void setMetadata_kind(String metadata_kind) {
        this.metadata_kind = metadata_kind;
    }
}
