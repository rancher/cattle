package io.cattle.platform.configitem.context.data;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DnsEntryData {
    IpAddress sourceIpAddress;
    Map<String, List<String>> resolve = new HashMap<>();
    Instance instance;

    public DnsEntryData() {
    }

    public IpAddress getSourceIpAddress() {
        return sourceIpAddress;
    }

    public void setSourceIpAddress(IpAddress sourceIpAddress) {
        this.sourceIpAddress = sourceIpAddress;
    }


    public Map<String, List<String>> getResolve() {
        return resolve;
    }

    public void setResolve(Map<String, List<String>> resolve) {
        this.resolve = resolve;
    }

    public Instance getInstance() {
        return instance;
    }

    public void setInstance(Instance instance) {
        this.instance = instance;
    }
}
