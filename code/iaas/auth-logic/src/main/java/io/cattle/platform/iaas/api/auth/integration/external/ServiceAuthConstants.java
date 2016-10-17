package io.cattle.platform.iaas.api.auth.integration.external;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringProperty;

public class ServiceAuthConstants {
    public static DynamicStringProperty AUTH_SERVICE_URL = ArchaiusUtil.getString("system.stack.auth.url");
    public static final String ACCEPT = "Accept";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String AUTH_ERROR = "AuthError";
    public static final String JWT_KEY = "jwt";
    public static final String AUTHORIZATION = "Authorization";
    public static final String APPLICATION_JSON = "application/json";
    public static final String APPLICATION_FORM_URL_ENCODED = "application/x-www-form-urlencoded";
    public static final String ACCESSMODE_SETTING = "api.auth.access.mode";
    public static final String ALLOWED_IDENTITIES_SETTING = "api.auth.allowed.identities";
    public static final String USERTYPE_SETTING = "api.auth.user.type";
    public static final String IDENTITY_SEPARATOR_SETTING = "api.auth.external.provider.identity.separator";
    public static final String EXTERNAL_AUTH_PROVIDER_SETTING = "api.auth.external.provider.configured";
    public static final String JWT_CREATION_FAILED = "FailedToMakeJWT";
    public static final String NO_IDENTITY_LOOKUP_SETTING = "api.auth.external.provider.no.identity.lookup";

    public static final DynamicStringProperty ACCESS_MODE = ArchaiusUtil.getString(ACCESSMODE_SETTING);
    public static final DynamicStringProperty ALLOWED_IDENTITIES = ArchaiusUtil.getString(ALLOWED_IDENTITIES_SETTING);
    public static final DynamicStringProperty USER_TYPE = ArchaiusUtil.getString(USERTYPE_SETTING);
    public static final DynamicStringProperty IDENTITY_SEPARATOR = ArchaiusUtil.getString(IDENTITY_SEPARATOR_SETTING);
    public static final DynamicBooleanProperty IS_EXTERNAL_AUTH_PROVIDER = ArchaiusUtil.getBoolean(EXTERNAL_AUTH_PROVIDER_SETTING);
    public static final DynamicBooleanProperty NO_IDENTITY_LOOKUP_SUPPORTED = ArchaiusUtil.getBoolean(NO_IDENTITY_LOOKUP_SETTING);

}
