package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.List;

@Type(list = false, name = "lbConfig")
public class LbConfig {
    String config;
    List<PortRule> portRules;
    List<Long> certificateIds;
    Long defaultCertificateId;
    LoadBalancerCookieStickinessPolicy stickinessPolicy;

    public LbConfig(String config, List<PortRule> portRules, List<Long> certificateIds, Long defaultCertificateId,
            LoadBalancerCookieStickinessPolicy stickinessPolicy) {
        super();
        this.config = config;
        this.portRules = portRules;
        this.certificateIds = certificateIds;
        this.defaultCertificateId = defaultCertificateId;
        this.stickinessPolicy = stickinessPolicy;
    }

    public LbConfig() {
    }

    @Field(nullable = true)
    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public List<PortRule> getPortRules() {
        return portRules;
    }

    public void setPortRules(List<PortRule> portRules) {
        this.portRules = portRules;
    }

    @Field(typeString = "array[reference[certificate]]", nullable = true)
    public List<Long> getCertificateIds() {
        return certificateIds;
    }

    public void setCertificateIds(List<Long> certificateIds) {
        this.certificateIds = certificateIds;
    }

    @Field(typeString = "reference[certificate]", nullable = true)
    public Long getDefaultCertificateId() {
        return defaultCertificateId;
    }

    public void setDefaultCertificateId(Long defaultCertificateId) {
        this.defaultCertificateId = defaultCertificateId;
    }

    @Field(nullable = true)
    public LoadBalancerCookieStickinessPolicy getStickinessPolicy() {
        return stickinessPolicy;
    }

    public void setStickinessPolicy(LoadBalancerCookieStickinessPolicy stickinessPolicy) {
        this.stickinessPolicy = stickinessPolicy;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((certificateIds == null) ? 0 : certificateIds.hashCode());
        result = prime * result + ((config == null) ? 0 : config.hashCode());
        result = prime * result + ((defaultCertificateId == null) ? 0 : defaultCertificateId.hashCode());
        result = prime * result + ((portRules == null) ? 0 : portRules.hashCode());
        result = prime * result + ((stickinessPolicy == null) ? 0 : stickinessPolicy.hashCode());
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
        LbConfig other = (LbConfig) obj;
        if (certificateIds == null) {
            if (other.certificateIds != null)
                return false;
        } else if (!certificateIds.equals(other.certificateIds))
            return false;
        if (config == null) {
            if (other.config != null)
                return false;
        } else if (!config.equals(other.config))
            return false;
        if (defaultCertificateId == null) {
            if (other.defaultCertificateId != null)
                return false;
        } else if (!defaultCertificateId.equals(other.defaultCertificateId))
            return false;
        if (portRules == null) {
            if (other.portRules != null)
                return false;
        } else if (!portRules.equals(other.portRules))
            return false;
        if (stickinessPolicy == null) {
            if (other.stickinessPolicy != null)
                return false;
        } else if (!stickinessPolicy.equals(other.stickinessPolicy))
            return false;
        return true;
    }

}
