package io.cattle.platform.iaas.api.auth.integration.local;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import com.netflix.config.DynamicStringProperty;

public class LocalAuthConstants {
    public static final String NAME = "localAuth";
    public static final String CONFIG = NAME + "Config";
    public static final String PASSWORD = "password";
    public static final String JWT = NAME + "JWT";

    public static final String ACCESS_MODE_SETTING = "api.auth.local.access.mode";
    public static final DynamicStringProperty ACCESS_MODE = ArchaiusUtil.getString(ACCESS_MODE_SETTING);


    public static final String TOKEN_CREATOR = NAME + "TokenCreator";
    public static final String AUTH_IMPL = NAME + "AuthImpl";
}
