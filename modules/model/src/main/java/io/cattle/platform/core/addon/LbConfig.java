package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.List;

@Type(list = false)
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

    @Field(typeString = "array[reference[certificate]]")
    public List<Long> getCertificateIds() {
        return certificateIds;
    }

    public void setCertificateIds(List<Long> certificateIds) {
        this.certificateIds = certificateIds;
    }

    @Field(typeString = "reference[certificate]")
    public Long getDefaultCertificateId() {
        return defaultCertificateId;
    }

    public void setDefaultCertificateId(Long defaultCertificateId) {
        this.defaultCertificateId = defaultCertificateId;
    }

    public LoadBalancerCookieStickinessPolicy getStickinessPolicy() {
        return stickinessPolicy;
    }

    public void setStickinessPolicy(LoadBalancerCookieStickinessPolicy stickinessPolicy) {
        this.stickinessPolicy = stickinessPolicy;
    }

}
