package io.cattle.platform.iaas.api.auth.integration.ldap;

import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.integration.ldap.interfaces.LDAPConstants;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.net.ConnectException;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LDAPUtils {

    private static final Log logger = LogFactory.getLog(LDAPUtils.class);

    private static final String INVALID_OPEN_LDAP_CONFIG = "InvalidLDAPConfig";
    private static final Pattern LDAP_ERROR_CODE = Pattern.compile("data +[57][2307][01235e]");



    /**
     * @throws ClientVisibleException In the event that the provided configuration is invalid. This allows the user to fix the config
     * and resubmit it, without locking them out of cattle.
     */
    public static void validateConfig(LDAPConstants ldapConfig) {
        if (ldapConfig.getEnabled() == null || !ldapConfig.getEnabled() || SecurityConstants.SECURITY.get()) {
            return;
        }
        if (StringUtils.isBlank(ldapConfig.getServiceAccountUsername()) || StringUtils.isBlank(ldapConfig.getServiceAccountPassword())) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "InvalidLDAPConfig", "Must define service account.",
                    "Cannot have a config with blank service account user name or password.");
        }
        LdapContext userContext = null;
        try {
            Hashtable<String, String> props = new Hashtable<>();
            props.put(Context.SECURITY_AUTHENTICATION, "simple");
            String username = ldapConfig.getServiceAccountUsername();
            if (StringUtils.isNotBlank(ldapConfig.getLoginDomain()) && !username.contains("\\")) {
                username = ldapConfig.getLoginDomain() + '\\' +username;
            }
            props.put(Context.SECURITY_PRINCIPAL, username);
            props.put(Context.SECURITY_CREDENTIALS, ldapConfig.getServiceAccountPassword());
            props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            props.put("com.sun.jndi.ldap.connect.timeout", String.valueOf(ldapConfig.getConnectionTimeout()));
            String url = "ldap://" + ldapConfig.getServer() + ':' + String.valueOf(ldapConfig.getPort()) + '/';
            props.put(Context.PROVIDER_URL, url);
            if (ldapConfig.getTls()) {
                props.put(Context.SECURITY_PROTOCOL, "ssl");
            }
            userContext = new InitialLdapContext(props, null);
            try {
                userContext.getAttributes(new LdapName(ldapConfig.getDomain()));
            } catch (NamingException e) {
                throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, INVALID_OPEN_LDAP_CONFIG, "Invalid ldap search base.",
                        "Unable to get provided Ldap Search base: " + ldapConfig.getDomain());
            }
        } catch (NamingException e) {
            if (e.getRootCause() instanceof ConnectException){
                throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, INVALID_OPEN_LDAP_CONFIG,
                        "Unable to talk to ldap.", "Provided server and port refused connection.");
            }
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, INVALID_OPEN_LDAP_CONFIG,
                    "Unable to authenticate service account.", "Unable to create context with service account credentials." +
                    "Username(DN):" + ldapConfig.getServiceAccountUsername() + " Password:" + ldapConfig.getServiceAccountPassword());
        }finally {
            if (userContext != null) {
                try {
                    userContext.close();
                } catch (NamingException e) {
                    logger.info("Failed to close Test service context.", e);
                }
            }
        }
    }

    public static String errorCodeToDescription(NamingException code){
        String errorCode = code.getExplanation();
        Matcher m = LDAP_ERROR_CODE.matcher(errorCode);
        if (m.find()) {
            errorCode = m.group(0).substring(m.group(0).length()-3);
        }
        switch (errorCode) {
            case "525":
                return "User not found";
            case "52e":
                return "Invalid credentials";
            case "530":
                return "Not permitted to logon at this time";
            case "531":
                return "Not permitted to logon at this workstation";
            case "532":
                return "Password expired (remember to check the user set in osuser.xml also)";
            case "533":
                return "Account disabled";
            case "701":
                return "Account expired";
            case "773":
                return "User must reset password";
            case "775":
                return "User account locked";
            default:
                return errorCode;
        }
    }
}
