package io.cattle.platform.metadata.model;

import io.cattle.platform.core.addon.PortBinding;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.object.util.DataAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ServiceInfo implements MetadataObject  {

    long id;
    Long stackId;

    Integer scale;

    String fqdn;
    String token;
    String name;
    String uuid;
    String kind;
    String hostname;
    String vip;
    String state;
    String healthState;

    List<String> externalIps;
    List<String> sidekicks;
    List<PortBinding> ports = new ArrayList<>();
    Map<String, Object> links;
    Map<String, String> labels;
    Map<String, Object> metadata;

    boolean system;
    //LBConfigMetadataStyle lb_config;

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
        this.ports = DataAccessor.fieldObjectList(service, ServiceConstants.FIELD_PUBLIC_ENDPOINTS, PortBinding.class);
        this.links = DataAccessor.fieldMapRO(service, ServiceConstants.FIELD_SERVICE_LINKS);
        this.labels = DataAccessor.getLabels(service);
        this.metadata = DataAccessor.fieldMapRO(service, ServiceConstants.FIELD_METADATA);
        this.system = service.getSystem();
    }

    public long getId() {
        return id;
    }

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

    public List<PortBinding> getPorts() {
        return ports;
    }

    public Map<String, Object> getLinks() {
        return links;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public boolean isSystem() {
        return system;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((externalIps == null) ? 0 : externalIps.hashCode());
        result = prime * result + ((fqdn == null) ? 0 : fqdn.hashCode());
        result = prime * result + ((healthState == null) ? 0 : healthState.hashCode());
        result = prime * result + ((hostname == null) ? 0 : hostname.hashCode());
        result = prime * result + (int) (id ^ (id >>> 32));
        result = prime * result + ((kind == null) ? 0 : kind.hashCode());
        result = prime * result + ((labels == null) ? 0 : labels.hashCode());
        result = prime * result + ((links == null) ? 0 : links.hashCode());
        result = prime * result + ((metadata == null) ? 0 : metadata.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((ports == null) ? 0 : ports.hashCode());
        result = prime * result + ((scale == null) ? 0 : scale.hashCode());
        result = prime * result + ((sidekicks == null) ? 0 : sidekicks.hashCode());
        result = prime * result + ((stackId == null) ? 0 : stackId.hashCode());
        result = prime * result + ((state == null) ? 0 : state.hashCode());
        result = prime * result + (system ? 1231 : 1237);
        result = prime * result + ((token == null) ? 0 : token.hashCode());
        result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
        result = prime * result + ((vip == null) ? 0 : vip.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ServiceInfo other = (ServiceInfo) obj;
        if (externalIps == null) {
            if (other.externalIps != null)
                return false;
        } else if (!externalIps.equals(other.externalIps))
            return false;
        if (fqdn == null) {
            if (other.fqdn != null)
                return false;
        } else if (!fqdn.equals(other.fqdn))
            return false;
        if (healthState == null) {
            if (other.healthState != null)
                return false;
        } else if (!healthState.equals(other.healthState))
            return false;
        if (hostname == null) {
            if (other.hostname != null)
                return false;
        } else if (!hostname.equals(other.hostname))
            return false;
        if (id != other.id)
            return false;
        if (kind == null) {
            if (other.kind != null)
                return false;
        } else if (!kind.equals(other.kind))
            return false;
        if (labels == null) {
            if (other.labels != null)
                return false;
        } else if (!labels.equals(other.labels))
            return false;
        if (links == null) {
            if (other.links != null)
                return false;
        } else if (!links.equals(other.links))
            return false;
        if (metadata == null) {
            if (other.metadata != null)
                return false;
        } else if (!metadata.equals(other.metadata))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (ports == null) {
            if (other.ports != null)
                return false;
        } else if (!ports.equals(other.ports))
            return false;
        if (scale == null) {
            if (other.scale != null)
                return false;
        } else if (!scale.equals(other.scale))
            return false;
        if (sidekicks == null) {
            if (other.sidekicks != null)
                return false;
        } else if (!sidekicks.equals(other.sidekicks))
            return false;
        if (stackId == null) {
            if (other.stackId != null)
                return false;
        } else if (!stackId.equals(other.stackId))
            return false;
        if (state == null) {
            if (other.state != null)
                return false;
        } else if (!state.equals(other.state))
            return false;
        if (system != other.system)
            return false;
        if (token == null) {
            if (other.token != null)
                return false;
        } else if (!token.equals(other.token))
            return false;
        if (uuid == null) {
            if (other.uuid != null)
                return false;
        } else if (!uuid.equals(other.uuid))
            return false;
        if (vip == null) {
            if (other.vip != null)
                return false;
        } else if (!vip.equals(other.vip))
            return false;
        return true;
    }

}
