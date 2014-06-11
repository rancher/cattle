package io.cattle.platform.configitem.context.data;

import io.cattle.platform.configitem.context.impl.HostnameGenerator;

public class NetworkClientData {

    Integer clientDeviceNumber;
    String macAddress;
    Integer instanceDeviceNumber;
    String instanceDomain;
    String hostname;
    Long instanceId;
    String instanceUuid;
    String ipAddress;
    String networkDomain;
    String gateway;

    public String getFqdn() {
        return HostnameGenerator.lookup(true, hostname, instanceDomain, ipAddress, networkDomain);
    }

    public Integer getClientDeviceNumber() {
        return clientDeviceNumber;
    }

    public void setClientDeviceNumber(Integer clientDeviceNumber) {
        this.clientDeviceNumber = clientDeviceNumber;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public Integer getInstanceDeviceNumber() {
        return instanceDeviceNumber;
    }

    public void setInstanceDeviceNumber(Integer instanceDeviceNumber) {
        this.instanceDeviceNumber = instanceDeviceNumber;
    }

    public String getInstanceDomain() {
        return instanceDomain;
    }

    public void setInstanceDomain(String instanceDomain) {
        this.instanceDomain = instanceDomain;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Long getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(Long instanceId) {
        this.instanceId = instanceId;
    }

    public String getInstanceUuid() {
        return instanceUuid;
    }

    public void setInstanceUuid(String instanceUuid) {
        this.instanceUuid = instanceUuid;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getNetworkDomain() {
        return networkDomain;
    }

    public void setNetworkDomain(String networkDomain) {
        this.networkDomain = networkDomain;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }
}
