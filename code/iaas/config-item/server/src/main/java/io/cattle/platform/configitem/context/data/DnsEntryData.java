package io.cattle.platform.configitem.context.data;

import io.cattle.platform.core.model.Instance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

public class DnsEntryData {
    String sourceIpAddress;
    Map<String, Map<String, String>> resolveServicesAndContainers = new HashMap<>();
    Map<String, List<String>> resolve = new HashMap<>();
    Map<String, String> resolveCname = new HashMap<>();
    Instance instance;

    public DnsEntryData() {
    }

    public String getSourceIpAddress() {
        return sourceIpAddress;
    }

    public void setSourceIpAddress(String sourceIpAddress) {
        this.sourceIpAddress = sourceIpAddress;
    }


    public Map<String, Map<String, String>> getResolveServicesAndContainers() {
        return resolveServicesAndContainers;
    }

    public void setResolveServicesAndContainers(Map<String, Map<String, String>> resolve) {
        this.resolveServicesAndContainers = resolve;
        for (String serviceName : resolve.keySet()) {
            this.resolve.put(serviceName, Lists.newArrayList(resolve.get(serviceName).keySet()));
            for (String ipAddress : resolve.get(serviceName).keySet()) {
                String instanceName = resolve.get(serviceName).get(ipAddress);
                if (instanceName != null) {
                    List<String> ips = new ArrayList<>();
                    ips.add(ipAddress);
                    this.resolve.put(instanceName, ips);
                }
            }
        }
    }

    public Instance getInstance() {
        return instance;
    }

    public void setInstance(Instance instance) {
        this.instance = instance;
    }

    public Map<String, String> getResolveCname() {
        return resolveCname;
    }

    public void setResolveCname(Map<String, String> resolveCname) {
        this.resolveCname = resolveCname;
    }

    public Map<String, List<String>> getResolve() {
        return resolve;
    }

    public void setResolve(Map<String, List<String>> resolve) {
        this.resolve = resolve;
    }
}
