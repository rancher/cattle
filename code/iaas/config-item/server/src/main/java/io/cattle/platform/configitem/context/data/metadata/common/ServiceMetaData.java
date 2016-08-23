package io.cattle.platform.configitem.context.data.metadata.common;

import io.cattle.platform.configitem.context.dao.MetaDataInfoDao;
import io.cattle.platform.configitem.context.dao.MetaDataInfoDao.Version;
import io.cattle.platform.configitem.context.data.metadata.version1.ServiceMetaDataVersion1;
import io.cattle.platform.configitem.context.data.metadata.version2.ServiceMetaDataVersion2;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ServiceMetaData {
    private Long serviceId;
    private boolean isPrimaryConfig;
    private String launchConfigName;
    private Long stackId;
    private Service service;
    
    protected String name;
    protected String uuid;
    protected String stack_name;
    protected String kind;
    protected String hostname;
    protected String vip;
    protected Long create_index;
    protected List<String> external_ips = new ArrayList<>();
    protected List<String> sidekicks;
    protected List<ContainerMetaData> containers = new ArrayList<>();
    protected Map<String, String> links;
    protected List<String> ports = new ArrayList<>();
    protected Map<String, String> labels;
    protected Map<String, Object> metadata;
    protected Integer scale;
    protected String fqdn;
    protected List<String> expose = new ArrayList<>();
    protected String token;

    protected ServiceMetaData(ServiceMetaData that) {
        this.name = that.name;
        this.uuid = that.uuid;
        this.stack_name = that.stack_name;
        this.kind = that.kind;
        this.hostname = that.hostname;
        this.vip = that.vip;
        this.create_index = that.create_index;
        this.external_ips = that.external_ips;
        this.sidekicks = that.sidekicks;
        this.containers = that.containers;
        this.links = that.links;
        this.ports = that.ports;
        this.labels = that.labels;
        this.metadata = that.metadata;
        this.scale = that.scale;
        this.fqdn = that.fqdn;
        this.expose = that.expose;
        this.serviceId = that.serviceId;
        this.service = that.service;
        this.isPrimaryConfig = that.isPrimaryConfig;
        this.launchConfigName = that.launchConfigName;
        this.stackId = that.stackId;
    }

    public ServiceMetaData(Service service, String serviceName, Stack env, List<String> sidekicks,
            Map<String, Object> metadata) {
        this.serviceId = service.getId();
        this.service = service;
        this.name = serviceName;
        this.uuid = service.getUuid();
        this.stack_name = env.getName();
        this.stackId = env.getId();
        this.kind = service.getKind();
        this.sidekicks = sidekicks;
        this.vip = service.getVip();
        this.isPrimaryConfig = service.getName().equalsIgnoreCase(serviceName);
        String launchConfigName = this.isPrimaryConfig ? ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME
                : serviceName;
        this.launchConfigName = launchConfigName;
        this.labels = ServiceDiscoveryUtil.getLaunchConfigLabels(service, launchConfigName);
        populateExternalServiceInfo(service);
        populatePortsInfo(service, launchConfigName);
        this.create_index = service.getCreateIndex();
        this.metadata = metadata;
        this.scale = DataAccessor.fieldInteger(service, ServiceDiscoveryConstants.FIELD_SCALE);
        this.fqdn = DataAccessor.fieldString(service, ServiceDiscoveryConstants.FIELD_FQDN);
        this.stackId = env.getId();
        Integer desiredScale = DataAccessor.fieldInteger(service, ServiceDiscoveryConstants.FIELD_DESIRED_SCALE);
        if (desiredScale != null) {
            this.scale = desiredScale;
        }
    }

    @SuppressWarnings("unchecked")
    protected void populatePortsInfo(Service service, String serviceName) {
        Object portsObj = ServiceDiscoveryUtil.getLaunchConfigObject(service, serviceName,
                InstanceConstants.FIELD_PORTS);
        if (portsObj != null) {
            this.ports.addAll((List<String>) portsObj);
        }
        Object exposeObj = ServiceDiscoveryUtil.getLaunchConfigObject(service, serviceName,
                InstanceConstants.FIELD_EXPOSE);
        if (exposeObj != null) {
            this.expose.addAll((List<String>) exposeObj);
        }
    }

    @SuppressWarnings("unchecked")
    protected void populateExternalServiceInfo(Service service) {
        if (kind.equalsIgnoreCase(ServiceDiscoveryConstants.KIND_EXTERNAL_SERVICE)) {
            this.hostname = DataAccessor.fields(service)
                    .withKey(ServiceDiscoveryConstants.FIELD_HOSTNAME).as(String.class);
            external_ips.addAll(DataAccessor.fields(service)
                    .withKey(ServiceDiscoveryConstants.FIELD_EXTERNALIPS).withDefault(Collections.EMPTY_LIST)
                    .as(List.class));
        }
    }

    public String getName() {
        return name;
    }

    public String getStack_name() {
        return stack_name;
    }

    public String getKind() {
        return kind;
    }

    public String getHostname() {
        return hostname;
    }

    public String getVip() {
        return vip;
    }

    public List<String> getExternal_ips() {
        return external_ips;
    }

    public List<String> getSidekicks() {
        return sidekicks;
    }

    public Map<String, String> getLinks() {
        return links;
    }

    public List<String> getPorts() {
        return ports;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public boolean isPrimaryConfig() {
        return isPrimaryConfig;
    }

    public void setLinks(Map<String, String> links) {
        this.links = links;
    }

    public String getLaunchConfigName() {
        return launchConfigName;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Integer getScale() {
        return scale;
    }

    public String getFqdn() {
        return fqdn;
    }

    public String getUuid() {
        return uuid;
    }

    public List<String> getExpose() {
        return expose;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setStack_name(String stack_name) {
        this.stack_name = stack_name;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setVip(String vip) {
        this.vip = vip;
    }

    public void setExternal_ips(List<String> external_ips) {
        this.external_ips = external_ips;
    }

    public void setSidekicks(List<String> sidekicks) {
        this.sidekicks = sidekicks;
    }

    public void setPorts(List<String> ports) {
        this.ports = ports;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public void setScale(Integer scale) {
        this.scale = scale;
    }

    public void setFqdn(String fqdn) {
        this.fqdn = fqdn;
    }

    public void setExpose(List<String> expose) {
        this.expose = expose;
    }

    public void setContainersObj(List<ContainerMetaData> containers) {
        this.containers = containers;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public Service getService() {
        return service;
    }

    public static ServiceMetaData getServiceMetaData(ServiceMetaData serviceData, Version version) {
        if (version == MetaDataInfoDao.Version.version1) {
            return new ServiceMetaDataVersion1(serviceData);
        } else {
            return new ServiceMetaDataVersion2(serviceData);
        }
    }

    
}
