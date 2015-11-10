package io.cattle.platform.configitem.context.data;


public class DnsResolveEntryData {
    String dnsName;
    String ipAddress;
    Long serviceId;
    String targetInstanceName;

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public String getDnsName() {
        return dnsName;
    }

    public void setDnsName(String dnsName) {
        this.dnsName = dnsName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getTargetInstanceName() {
        return targetInstanceName;
    }

    public void setTargetInstanceName(String targetInstanceName) {
        this.targetInstanceName = targetInstanceName;
    }
}
