package io.cattle.platform.iaas.api.auth.integration.ldap.ad;

import io.cattle.platform.iaas.api.auth.integration.ldap.ServiceContextCreationException;
import io.cattle.platform.iaas.api.auth.integration.ldap.interfaces.LDAPConstants;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;


public class LdapServiceContextPoolFactory implements PooledObjectFactory<LdapContext> {

    private static final Log logger = LogFactory.getLog(LdapServiceContextPoolFactory.class);
    LDAPConstants config;

    public LdapServiceContextPoolFactory(LDAPConstants config) {
        this.config = config;
    }

    @Override
    public PooledObject<LdapContext> makeObject() throws Exception {
        String username = config.getServiceAccountUsername();
        if (StringUtils.isNotBlank(config.getLoginDomain()) && !username.contains("\\")) {
            username = config.getLoginDomain() + '\\' +username;
        }
        Hashtable<String, String> props = new Hashtable<>();
        props.put(Context.SECURITY_AUTHENTICATION, "simple");
        props.put(Context.SECURITY_PRINCIPAL, username);
        props.put(Context.SECURITY_CREDENTIALS, config.getServiceAccountPassword());
        props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        props.put("com.sun.jndi.ldap.connect.timeout", String.valueOf(config.getConnectionTimeout()));
        LdapContext userContext;

        try {
            String url = "ldap://" + config.getServer() + ':' + config.getPort() + '/';
            props.put(Context.PROVIDER_URL, url);
            if (config.getTls()) {
                props.put(Context.SECURITY_PROTOCOL, "ssl");
            }
            userContext = new InitialLdapContext(props, null);
        } catch (NamingException e) {
            logger.info("Failed to create a service context.");
            logger.info(e.getMessage());
            throw new ServiceContextCreationException("Unable to login to ldap using configured Service account.", e);
        }
        return new DefaultPooledObject<>(userContext);
    }

    @Override
    public void destroyObject(PooledObject<LdapContext> p) throws Exception {
        p.getObject().close();
    }

    @Override
    public boolean validateObject(PooledObject<LdapContext> p) {
        try {
            p.getObject().getAttributes(new LdapName(config.getDomain()));
            return true;
        } catch (NamingException e) {
            logger.info("Failed to validate an existing ldap service context.");
            logger.info(e.getMessage());
            return false;
        }
    }

    @Override
    public void activateObject(PooledObject<LdapContext> p) throws Exception {
    }

    @Override
    public void passivateObject(PooledObject<LdapContext> p) throws Exception {
    }
}
