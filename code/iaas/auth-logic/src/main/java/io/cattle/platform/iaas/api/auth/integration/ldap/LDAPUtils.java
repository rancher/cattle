package io.cattle.platform.iaas.api.auth.integration.ldap;

import io.cattle.platform.iaas.api.auth.integration.ldap.interfaces.LDAPConfig;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.net.ConnectException;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LDAPUtils {

    private static final Log logger = LogFactory.getLog(LDAPUtils.class);

    private static final String INVALID_OPEN_LDAP_CONFIG = "InvalidLDAPConfig";


    /**
     * @throws ClientVisibleException In the event that the provided configuration is invalid. This allows the user to fix the config
     * and resubmit it, without locking them out of cattle.
     */
    public static void validateConfig(LDAPConfig ldapConfig) {
        LdapContext userContext = null;
        try {
            Hashtable<String, String> props = new Hashtable<>();
            props.put(Context.SECURITY_AUTHENTICATION, "simple");
            props.put(Context.SECURITY_PRINCIPAL, ldapConfig.getServiceAccountUsername());
            props.put(Context.SECURITY_CREDENTIALS, ldapConfig.getServiceAccountPassword());
            props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
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
}
