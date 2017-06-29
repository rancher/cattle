package io.cattle.platform.iaas.api.auth;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.iaas.api.auth.integration.azure.AzureConstants;
import io.cattle.platform.iaas.api.auth.integration.ldap.OpenLDAP.OpenLDAPConstants;
import io.cattle.platform.iaas.api.auth.integration.ldap.ad.ADConstants;
import io.cattle.platform.iaas.api.auth.integration.local.LocalAuthConstants;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicStringProperty;

public class SecurityConstants {

    private final static SecureRandom RANDOM = new SecureRandom();

    public static final String ENABLED = "enabled";
    public static final String SECURITY_SETTING = "api.security.enabled";
    public static final DynamicBooleanProperty SECURITY = ArchaiusUtil.getBoolean(SECURITY_SETTING);
    public static final String AUTH_PROVIDER_SETTING = "api.auth.provider.configured";
    public static final DynamicStringProperty AUTH_PROVIDER = ArchaiusUtil.getString(AUTH_PROVIDER_SETTING);
    public static final String ROLE_SETTING_BASE = "api.security.role.priority.";
    public static final DynamicStringProperty BAD_CHARACTERS = ArchaiusUtil.getString("process.credential.create.bad.characters");

    public static final String NO_PROVIDER = "none";
    public static final String CODE = "code";
    public static final String TOKEN_VERSION = "v1";
    public static final DynamicLongProperty TOKEN_EXPIRY_MILLIS = ArchaiusUtil.getLong("api.auth.jwt.token.expiry");
    public static final String HAS_LOGGED_IN = "hasLoggedIn";
    public static final String AUTH_ENABLER = "api.auth.enabler";
    public static final DynamicStringProperty AUTH_ENABLER_SETTING = ArchaiusUtil.getString(AUTH_ENABLER);
    public static final List<String> INTERNAL_AUTH_PROVIDERS = Arrays.asList(
            AzureConstants.CONFIG,
            ADConstants.CONFIG,
            OpenLDAPConstants.CONFIG,
            LocalAuthConstants.CONFIG);

    public static String[] generateKeys() {
        byte[] accessKey = new byte[10];
        byte[] secretKey = new byte[128];

        RANDOM.nextBytes(accessKey);
        RANDOM.nextBytes(secretKey);

        String accessKeyString = Hex.encodeHexString(accessKey);
        String secretKeyString = Base64.encodeBase64String(secretKey).replaceAll(BAD_CHARACTERS.get(), "");

        if (secretKeyString.length() < 40) {
            /* Wow, this is terribly bad luck */
            throw new IllegalStateException("Failed to create secretKey due to not enough good characters");
        }

        return new String[] { accessKeyString.substring(0, 20).toUpperCase(), secretKeyString.substring(0, 40) };
    }

}
