package io.cattle.platform.iaas.api.auth;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicStringProperty;

public class SecurityConstants {

    public static final String ENABLED = "enabled";
    public static final String SECURITY_SETTING = "api.security.enabled";
    public static final DynamicBooleanProperty SECURITY = ArchaiusUtil.getBoolean(SECURITY_SETTING);
    public static final String AUTH_PROVIDER_SETTING = "api.auth.provider.configured";
    public static final DynamicStringProperty AUTH_PROVIDER = ArchaiusUtil.getString(AUTH_PROVIDER_SETTING);
    public static final String ROLE_SETTING_BASE = "api.security.role.priority.";


    public static final String NO_PROVIDER = "none";
    public static final String CODE = "code";
    public static final String TOKEN_VERSION = "v1";
    public static final DynamicLongProperty TOKEN_EXPIRY_MILLIS = ArchaiusUtil.getLong("api.auth.jwt.token.expiry");
}
