package io.cattle.platform.configitem.context.data;

import io.cattle.platform.core.model.IpAddress;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DnsEntryData {
    IpAddress sourceIpAddress;
    Map<String, List<IpAddress>> resolve = new HashMap<>();

    public DnsEntryData() {
    }

    public IpAddress getSourceIpAddress() {
        return sourceIpAddress;
    }

    public void setSourceIpAddress(IpAddress sourceIpAddress) {
        this.sourceIpAddress = sourceIpAddress;
    }


    public Map<String, List<IpAddress>> getResolve() {
        return resolve;
    }

    public void setResolve(Map<String, List<IpAddress>> resolve) {
        this.resolve = resolve;
    }
}
