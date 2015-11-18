package io.cattle.platform.iaas.api.auth.integration.ldap;

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

    @Override
    public PooledObject<LdapContext> makeObject() throws Exception {
        String username = LdapConstants.SERVICE_ACCOUNT_USER.get();
        if (StringUtils.isNotBlank(LdapConstants.LDAP_LOGIN_DOMAIN.get())) {
            username = LdapConstants.LDAP_LOGIN_DOMAIN.get() + '\\' +username;
        }
        Hashtable<String, String> props = new Hashtable<>();
        props.put(Context.SECURITY_AUTHENTICATION, "simple");
        props.put(Context.SECURITY_PRINCIPAL, username);
        props.put(Context.SECURITY_CREDENTIALS, LdapConstants.SERVICE_ACCOUNT_PASSWORD.get());
        props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        LdapContext userContext;

        try {
            String url = "ldap://" + LdapConstants.LDAP_SERVER.get() + ':' + LdapConstants.LDAP_PORT.get() + '/';
            props.put(Context.PROVIDER_URL, url);
            if (LdapConstants.TLS_ENABLED.get()) {
                props.put(Context.SECURITY_PROTOCOL, "ssl");
            }
            userContext = new InitialLdapContext(props, null);
        } catch (NamingException e) {
            logger.info("Failed to create a service context.", e);
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
            p.getObject().getAttributes(new LdapName(LdapConstants.LDAP_DOMAIN.get()));
            return true;
        } catch (NamingException e) {
            logger.info("Failed to validate an existing ldap service context.", e);
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
