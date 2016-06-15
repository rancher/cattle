package io.cattle.platform.iaas.api.auth.integration.azure;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;


@Type(name = AzureConstants.CONFIG)
public class AzureConfig {
    private Boolean enabled;
    private String tenantId;    
    private String clientId;
    private String accessMode;  
    private String domain;  
    private String adminAccountUsername;
    private String adminAccountPassword;

    public AzureConfig(Boolean enabled, String accessMode, String tenantId, String clientId, 
            String domain, String adminAccountUsername, String adminAccountPassword) {
        this.enabled = enabled;
        this.tenantId = tenantId;
        this.clientId = clientId;
        this.accessMode = accessMode;
        this.domain = domain;
        this.adminAccountUsername = adminAccountUsername;
        this.adminAccountPassword = adminAccountPassword;
    }

    @Field(required = true, nullable = false, defaultValue = "unrestricted")
    public String getAccessMode() {
        return accessMode;
    }
    
    @Field(nullable = true)
    public Boolean getEnabled() {
        return enabled;
    }

    @Field(nullable = true)
    public String getClientId() {
        return clientId;
    }

    @Field(nullable = true)
    public String getTenantId() {
        return tenantId;
    }

    public String getName() {
        return AzureConstants.CONFIG;
    }
    
    @Field(nullable = true)
    public String getDomain() {
        return domain;
    }   
    
    @Field(nullable = true, required = true, minLength = 1)
    public String getAdminAccountUsername() {
        return adminAccountUsername;
    }

    @Field(nullable = true, required = true, minLength = 1)
    public String getAdminAccountPassword() {
        return adminAccountPassword;
    }    

}
