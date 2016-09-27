package io.cattle.platform.configitem.context.data.metadata.common;

import io.cattle.platform.configitem.context.dao.MetaDataInfoDao;
import io.cattle.platform.configitem.context.dao.MetaDataInfoDao.Version;
import io.cattle.platform.configitem.context.data.metadata.version1.ServiceMetaDataVersion1;
import io.cattle.platform.configitem.context.data.metadata.version2.ServiceMetaDataVersion2;
import io.cattle.platform.configitem.context.data.metadata.version2.ServiceMetaDataVersion3;
import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.util.LBMetadataUtil.LBConfigMetadataStyle;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceMetaData {

    public static class HealthCheck {
        Integer response_timeout;
        Integer interval;
        Integer healthy_threshold;
        Integer unhealthy_threshold;
        String request_line;
        Integer port;

        public HealthCheck(InstanceHealthCheck hc) {
            super();
            this.response_timeout = hc.getResponseTimeout();
            this.interval = hc.getInterval();
            this.healthy_threshold = hc.getHealthyThreshold();
            this.unhealthy_threshold = hc.getUnhealthyThreshold();
            this.request_line = hc.getRequestLine();
            this.port = hc.getPort();
        }

        public Integer getResponse_timeout() {
            return response_timeout;
        }

        public void setResponse_timeout(Integer response_timeout) {
            this.response_timeout = response_timeout;
        }

        public Integer getInterval() {
            return interval;
        }

        public void setInterval(Integer interval) {
            this.interval = interval;
        }

        public Integer getHealthy_threshold() {
            return healthy_threshold;
        }

        public void setHealthy_threshold(Integer healthy_threshold) {
            this.healthy_threshold = healthy_threshold;
        }

        public Integer getUnhealthy_threshold() {
            return unhealthy_threshold;
        }

        public void setUnhealthy_threshold(Integer unhealthy_threshold) {
            this.unhealthy_threshold = unhealthy_threshold;
        }

        public String getRequest_line() {
            return request_line;
        }

        public void setRequest_line(String request_line) {
            this.request_line = request_line;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }
    }

    private Long serviceId;
    private boolean isPrimaryConfig;
    private String launchConfigName;
    private Long stackId;
    private String stackUuid;
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
    protected HealthCheck health_check;
    protected Boolean system;
    protected LBConfigMetadataStyle lb_config;
    protected String primary_service_name;

    protected ServiceMetaData(ServiceMetaData that) {
        this.serviceId = that.serviceId;
        this.isPrimaryConfig = that.isPrimaryConfig;
        this.launchConfigName = that.launchConfigName;
        this.stackId = that.stackId;
        this.stackUuid = that.stackUuid;
        this.service = that.service;

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
        this.token = that.token;
        this.health_check = that.health_check;
        this.system = that.system;
        this.lb_config = that.lb_config;
        this.primary_service_name = that.primary_service_name;
    }

    public ServiceMetaData(Service service, String serviceName, Stack env, List<String> sidekicks,
            InstanceHealthCheck healthCheck, LBConfigMetadataStyle lbConfig) {
        this.serviceId = service.getId();
        this.service = service;
        this.name = serviceName;
        this.uuid = service.getUuid();
        this.stack_name = env.getName();
        this.stackId = env.getId();
        this.stackUuid = env.getUuid();
        this.kind = service.getKind();
        this.sidekicks = sidekicks;
        this.vip = getVip(service);
        this.isPrimaryConfig = service.getName().equalsIgnoreCase(serviceName);
        String launchConfigName = this.isPrimaryConfig ? ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME
                : serviceName;
        this.launchConfigName = launchConfigName;
        this.labels = ServiceDiscoveryUtil.getLaunchConfigLabels(service, launchConfigName);
        populateExternalServiceInfo(service);
        populatePortsInfo(service, launchConfigName);
        this.create_index = service.getCreateIndex();
        this.scale = DataAccessor.fieldInteger(service, ServiceConstants.FIELD_SCALE);
        this.fqdn = DataAccessor.fieldString(service, ServiceConstants.FIELD_FQDN);
        Integer desiredScale = DataAccessor.fieldInteger(service, ServiceConstants.FIELD_DESIRED_SCALE);
        if (desiredScale != null) {
            this.scale = desiredScale;
        }
        if (healthCheck != null) {
            this.health_check = new HealthCheck(healthCheck);
        }
        this.system = service.getSystem();
        this.metadata = DataAccessor.fieldMap(service, ServiceConstants.FIELD_METADATA);
        this.lb_config = lbConfig;
        this.primary_service_name = service.getName();
    }

    public static String getVip(Service service) {
        String vip = service.getVip();
        // indicator that its pre-upgraded setup that had vip set for every service by default
        // vip will be set only
        // a) field_set_vip is set via API
        // b) for k8s services
        Map<String, Object> data = new HashMap<>();
        data.putAll(DataUtils.getFields(service));
        Object vipObj = data.get(ServiceConstants.FIELD_SET_VIP);
        boolean setVip = vipObj != null && Boolean.valueOf(vipObj.toString());
        if (setVip
                || service.getKind().equalsIgnoreCase("kubernetesservice")) {
            return vip;
        }
        return null;
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
        if (kind.equalsIgnoreCase(ServiceConstants.KIND_EXTERNAL_SERVICE)) {
            this.hostname = DataAccessor.fields(service)
                    .withKey(ServiceConstants.FIELD_HOSTNAME).as(String.class);
            external_ips.addAll(DataAccessor.fields(service)
                    .withKey(ServiceConstants.FIELD_EXTERNALIPS).withDefault(Collections.EMPTY_LIST)
                    .as(List.class));
        }
    }

    public HealthCheck getHealth_check() {
        return health_check;
    }

    public void setHealth_check(HealthCheck health_check) {
        this.health_check = health_check;
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
        } else if (version == MetaDataInfoDao.Version.version2) {
            return new ServiceMetaDataVersion2(serviceData);
        } else {
            return new ServiceMetaDataVersion3(serviceData);
        }
    }

    public String getStackUuid() {
        return stackUuid;
    }

    public void setStackUuid(String stackUuid) {
        this.stackUuid = stackUuid;
    }

    public Boolean getSystem() {
        return system;
    }

    public void setSystem(Boolean system) {
        this.system = system;
    }

    public LBConfigMetadataStyle getLb_config() {
        return lb_config;
    }

    public void setLb_config(LBConfigMetadataStyle lb_config) {
        this.lb_config = lb_config;
    }
    
    public String getPrimary_service_name() {
        return primary_service_name;
    }

    public void setPrimary_service_name(String primary_service_name) {
        this.primary_service_name = primary_service_name;
    }
}