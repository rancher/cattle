package io.cattle.platform.iaas.api.auth.integration.local;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;
import io.github.ibuildthecloud.gdapi.model.FieldType;

@Type(name = LocalAuthConstants.CONFIG)
public class LocalAuthConfig {

    private final String username;
    private final String name;
    private final String password;
    private final String accessMode;
    private final boolean enabled;

    public LocalAuthConfig(String username, String name, String password, String accessMode, boolean enabled) {
        this.username = username;
        this.name = name;
        this.password = password;
        this.accessMode = accessMode;
        this.enabled = enabled;
    }

    @Field(nullable = false, type = FieldType.PASSWORD, required = true)
    public String getPassword() {
        return password;
    }

    @Field(nullable = false, required = true)
    public String getUsername() {
        return username;
    }

    @Field(nullable = true, required = false)
    public boolean isEnabled() {
        return enabled;
    }

    @Field(nullable = true, required = false, defaultValue = "admin")
    public String getName() {
        return name;
    }

    @Field(nullable = true, required = false, defaultValue = "unrestricted")
    public String getAccessMode() {
        return accessMode;
    }
}
