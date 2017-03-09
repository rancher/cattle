package io.cattle.platform.configitem.context.data.metadata.common;

import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.core.util.LBMetadataUtil.LBConfigMetadataStyle;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;

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
   
    String name;
    String uuid;
    String stack_name;
    String stack_uuid;
    String kind;
    String hostname;
    String vip;
    Long create_index;
    List<String> external_ips = new ArrayList<>();
    List<String> sidekicks;
    List<String> ports = new ArrayList<>();
    Map<String, String> labels;
    Map<String, Object> metadata;
    Integer scale;
    String fqdn;
    List<String> expose = new ArrayList<>();
    HealthCheck health_check;
    Boolean system;
    LBConfigMetadataStyle lb_config;
    String primary_service_name;
    String environment_uuid;
    String state;
    String token;
    // helper field needed by metadata service to process object
    String metadata_kind;

    public ServiceMetaData(Service service, String serviceName, String stackName, String stackUUID,
            List<String> sidekicks,
            InstanceHealthCheck healthCheck, LBConfigMetadataStyle lbConfig, Account account) {
        this.name = serviceName;
        this.uuid = service.getUuid();
        this.stack_name = stackName;
        this.stack_uuid = stackUUID;
        this.kind = service.getKind();
        this.sidekicks = sidekicks;
        this.vip = getVip(service);
        boolean isPrimaryConfig = service.getName().equalsIgnoreCase(serviceName);
        String launchConfigName = isPrimaryConfig ? ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME
                : serviceName;
        this.labels = ServiceUtil.getLaunchConfigLabels(service, launchConfigName);
        populateExternalServiceInfo(service);
        populatePortsInfo(service, launchConfigName);
        this.create_index = service.getCreateIndex();
        this.scale = DataAccessor.fieldInteger(service, ServiceConstants.FIELD_SCALE);
        this.fqdn = DataAccessor.fieldString(service, ServiceConstants.FIELD_FQDN);
        if (healthCheck != null) {
            this.health_check = new HealthCheck(healthCheck);
        }
        this.system = service.getSystem();
        this.metadata = DataAccessor.fieldMap(service, ServiceConstants.FIELD_METADATA);
        this.lb_config = lbConfig;
        this.primary_service_name = service.getName();
        this.environment_uuid = account.getUuid();
        this.state = service.getState();
        this.metadata_kind = "service";
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
    void populatePortsInfo(Service service, String serviceName) {
        Object portsObj = ServiceUtil.getLaunchConfigObject(service, serviceName,
                InstanceConstants.FIELD_PORTS);
        if (portsObj != null) {
            this.ports.addAll((List<String>) portsObj);
        }
        Object exposeObj = ServiceUtil.getLaunchConfigObject(service, serviceName,
                InstanceConstants.FIELD_EXPOSE);
        if (exposeObj != null) {
            this.expose.addAll((List<String>) exposeObj);
        }
    }

    @SuppressWarnings("unchecked")
    void populateExternalServiceInfo(Service service) {
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

    public List<String> getPorts() {
        return ports;
    }

    public Map<String, String> getLabels() {
        return labels;
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

    public String getStack_uuid() {
        return stack_uuid;
    }

    public void setStack_uuid(String stack_uuid) {
        this.stack_uuid = stack_uuid;
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

    public String getEnvironment_uuid() {
        return environment_uuid;
    }

    public void setEnvironment_uuid(String environment_uuid) {
        this.environment_uuid = environment_uuid;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Long getCreate_index() {
        return create_index;
    }

    public void setCreate_index(Long create_index) {
        this.create_index = create_index;
    }

    public String getMetadata_kind() {
        return metadata_kind;
    }

    public void setMetadata_kind(String metadata_kind) {
        this.metadata_kind = metadata_kind;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

}