package io.cattle.platform.core.addon;

public class ExternalCredential {

    String agentUuid;
    String publicValue;
    String secretValue;
    String environment;
    String region;
    Long regionId;
    
    public ExternalCredential() {
    }

    public ExternalCredential(String environmentName, String regionName, String publicValue, String secretValue, Long regionId) {
        super();
        this.publicValue = publicValue;
        this.secretValue = secretValue;
        this.environment = environmentName;
        this.region = regionName;
        this.regionId = regionId;
    }

    public String getPublicValue() {
        return publicValue;
    }

    public void setPublicValue(String publicValue) {
        this.publicValue = publicValue;
    }

    public String getSecretValue() {
        return secretValue;
    }

    public void setSecretValue(String secretValue) {
        this.secretValue = secretValue;
    }

    public String getEnvironmentName() {
        return environment;
    }

    public void setEnvironmentName(String environmentName) {
        this.environment = environmentName;
    }

    public String getRegionName() {
        return region;
    }

    public void setRegionName(String regionName) {
        this.region = regionName;
    }

    public String getAgentUuid() {
        return agentUuid;
    }

    public void setAgentUuid(String agentUuid) {
        this.agentUuid = agentUuid;
    }

    public Long getRegionId() {
        return regionId;
    }

}
