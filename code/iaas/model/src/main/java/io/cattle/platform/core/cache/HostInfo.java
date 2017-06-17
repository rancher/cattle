package io.cattle.platform.core.cache;

import io.cattle.platform.core.addon.PublicEndpoint;

import java.util.List;
import java.util.Map;

public class HostInfo {

    long id;
    String uuid;
    List<PublicEndpoint> ports;
    Map<String, String> labels;

    public HostInfo(long id, String uuid, Map<String, String> labels, List<PublicEndpoint> ports) {
        this.id = id;
        this.uuid = uuid;
        this.labels = labels;
        this.ports = ports;
    }

    public long getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public List<PublicEndpoint> getPorts() {
        return ports;
    }

}
