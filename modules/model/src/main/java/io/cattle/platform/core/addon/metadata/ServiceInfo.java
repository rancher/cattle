package io.cattle.platform.core.addon.metadata;

import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.addon.LbConfig;
import io.cattle.platform.core.addon.Link;
import io.cattle.platform.core.addon.PortInstance;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.annotation.Field;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ServiceInfo implements MetadataObject {

    long id;
    Long stackId;

    Integer scale;

    String environmentUuid;
    String fqdn;
    String token;
    String name;
    String uuid;
    String kind;
    String hostname;
    String vip;
    String state;
    String healthState;
    String selector;

    List<String> externalIps;
    List<String> sidekicks;
    Set<Long> instanceIds;
    Set<PortInstance> ports;
    List<Link> links;
    Map<String, String> labels;
    Map<String, Object> metadata;
    HealthcheckInfo healthCheck;
    LbConfig lbConfig;

    public ServiceInfo(Service service) {
        this.id = service.getId();
        this.stackId = service.getStackId();
        this.scale = DataAccessor.fieldInteger(service, ServiceConstants.FIELD_SCALE);
        this.fqdn = DataAccessor.fieldString(service, ServiceConstants.FIELD_FQDN);
        this.token = DataAccessor.fieldString(service, ServiceConstants.FIELD_TOKEN);
        this.name = service.getName();
        this.uuid = service.getUuid();
        this.kind = service.getKind();
        this.hostname = DataAccessor.fieldString(service, ServiceConstants.FIELD_HOSTNAME);
        this.healthState = service.getHealthState();
        this.externalIps = DataAccessor.fieldStringList(service, ServiceConstants.FIELD_EXTERNALIPS);
        this.sidekicks = ServiceUtil.getSidekickNames(service);
        this.ports = new HashSet<>(DataAccessor.fieldObjectList(service, ServiceConstants.FIELD_PUBLIC_ENDPOINTS, PortInstance.class));
        this.lbConfig = DataAccessor.field(service, ServiceConstants.FIELD_LB_CONFIG, LbConfig.class);
        this.links = DataAccessor.fieldObjectList(service, ServiceConstants.FIELD_SERVICE_LINKS, Link.class);
        this.labels = DataAccessor.getLabels(service);
        this.metadata = DataAccessor.fieldMapRO(service, ServiceConstants.FIELD_METADATA);
        this.selector = service.getSelector();
        this.instanceIds = new HashSet<>(DataAccessor.fieldLongList(service, ServiceConstants.FIELD_INSTANCE_IDS));
        InstanceHealthCheck hc = DataAccessor.field(service, InstanceConstants.FIELD_HEALTH_CHECK, InstanceHealthCheck.class);
        if (hc != null) {
            this.healthCheck = new HealthcheckInfo(hc);
        }
    }

    public LbConfig getLbConfig() {
        return lbConfig;
    }

    public HealthcheckInfo getHealthCheck() {
        return healthCheck;
    }

    @Field(typeString = "reference[service]")
    public Long getInfoTypeId() {
        return id;
    }

    @Override
    public String getInfoType() {
        return "service";
    }

    @Override
    public String getEnvironmentUuid() {
        return environmentUuid;
    }

    @Override
    public void setEnvironmentUuid(String environmentUuid) {
        this.environmentUuid = environmentUuid;
    }

    public boolean isGlobal() {
        return "true".equalsIgnoreCase(labels.get(SystemLabels.LABEL_SERVICE_GLOBAL));
    }

    public long getId() {
        return id;
    }

    @Field(typeString = "reference[stack]")
    public Long getStackId() {
        return stackId;
    }

    public Integer getScale() {
        return scale;
    }

    public String getFqdn() {
        return fqdn;
    }

    public String getToken() {
        return token;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getUuid() {
        return uuid;
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

    public String getState() {
        return state;
    }

    public String getHealthState() {
        return healthState;
    }

    public List<String> getExternalIps() {
        return externalIps;
    }

    public List<String> getSidekicks() {
        return sidekicks;
    }

    public List<Link> getLinks() {
        return links;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Set<PortInstance> getPorts() {
        return ports;
    }

    public String getSelector() {
        return selector;
    }

    @Field(typeString = "array[reference[instance]]")
    public Set<Long> getInstanceIds() {
        return instanceIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServiceInfo that = (ServiceInfo) o;

        if (id != that.id) return false;
        if (stackId != null ? !stackId.equals(that.stackId) : that.stackId != null) return false;
        if (scale != null ? !scale.equals(that.scale) : that.scale != null) return false;
        if (environmentUuid != null ? !environmentUuid.equals(that.environmentUuid) : that.environmentUuid != null)
            return false;
        if (fqdn != null ? !fqdn.equals(that.fqdn) : that.fqdn != null) return false;
        if (token != null ? !token.equals(that.token) : that.token != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (uuid != null ? !uuid.equals(that.uuid) : that.uuid != null) return false;
        if (kind != null ? !kind.equals(that.kind) : that.kind != null) return false;
        if (hostname != null ? !hostname.equals(that.hostname) : that.hostname != null) return false;
        if (vip != null ? !vip.equals(that.vip) : that.vip != null) return false;
        if (state != null ? !state.equals(that.state) : that.state != null) return false;
        if (healthState != null ? !healthState.equals(that.healthState) : that.healthState != null) return false;
        if (selector != null ? !selector.equals(that.selector) : that.selector != null) return false;
        if (externalIps != null ? !externalIps.equals(that.externalIps) : that.externalIps != null) return false;
        if (sidekicks != null ? !sidekicks.equals(that.sidekicks) : that.sidekicks != null) return false;
        if (instanceIds != null ? !instanceIds.equals(that.instanceIds) : that.instanceIds != null) return false;
        if (ports != null ? !ports.equals(that.ports) : that.ports != null) return false;
        if (links != null ? !links.equals(that.links) : that.links != null) return false;
        if (labels != null ? !labels.equals(that.labels) : that.labels != null) return false;
        if (metadata != null ? !metadata.equals(that.metadata) : that.metadata != null) return false;
        if (healthCheck != null ? !healthCheck.equals(that.healthCheck) : that.healthCheck != null) return false;
        return lbConfig != null ? lbConfig.equals(that.lbConfig) : that.lbConfig == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (stackId != null ? stackId.hashCode() : 0);
        result = 31 * result + (scale != null ? scale.hashCode() : 0);
        result = 31 * result + (environmentUuid != null ? environmentUuid.hashCode() : 0);
        result = 31 * result + (fqdn != null ? fqdn.hashCode() : 0);
        result = 31 * result + (token != null ? token.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (uuid != null ? uuid.hashCode() : 0);
        result = 31 * result + (kind != null ? kind.hashCode() : 0);
        result = 31 * result + (hostname != null ? hostname.hashCode() : 0);
        result = 31 * result + (vip != null ? vip.hashCode() : 0);
        result = 31 * result + (state != null ? state.hashCode() : 0);
        result = 31 * result + (healthState != null ? healthState.hashCode() : 0);
        result = 31 * result + (selector != null ? selector.hashCode() : 0);
        result = 31 * result + (externalIps != null ? externalIps.hashCode() : 0);
        result = 31 * result + (sidekicks != null ? sidekicks.hashCode() : 0);
        result = 31 * result + (instanceIds != null ? instanceIds.hashCode() : 0);
        result = 31 * result + (ports != null ? ports.hashCode() : 0);
        result = 31 * result + (links != null ? links.hashCode() : 0);
        result = 31 * result + (labels != null ? labels.hashCode() : 0);
        result = 31 * result + (metadata != null ? metadata.hashCode() : 0);
        result = 31 * result + (healthCheck != null ? healthCheck.hashCode() : 0);
        result = 31 * result + (lbConfig != null ? lbConfig.hashCode() : 0);
        return result;
    }
}
