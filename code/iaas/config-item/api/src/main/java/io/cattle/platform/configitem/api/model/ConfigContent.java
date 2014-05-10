package io.cattle.platform.configitem.api.model;

import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(pluralName = "configContent")
public interface ConfigContent {

    String getId();

    String getVersion();

    String getCurrent();

    Long getAgentId();

}
