package io.cattle.platform.configitem.context.data.metadata.common;

import java.util.Map;

public class NetworkMetaData {
    String name;
    String uuid;
    protected Map<String, Object> metadata;

    public NetworkMetaData(String name, String uuid, Map<String, Object> metadata) {
        super();
        this.name = name;
        this.uuid = uuid;
        this.metadata = metadata;
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

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

}
