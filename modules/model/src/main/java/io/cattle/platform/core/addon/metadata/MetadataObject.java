package io.cattle.platform.core.addon.metadata;

public interface MetadataObject {

    String getUuid();

    String getEnvironmentUuid();

    String getInfoType();

    void setEnvironmentUuid(String uuid);

}
