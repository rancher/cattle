package io.cattle.platform.iaas.api.auth.integration.ldap;

import static javax.naming.directory.SearchControls.*;
import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.constants.IdentityConstants;
import io.cattle.platform.iaas.api.auth.AbstractTokenUtil;
import io.cattle.platform.iaas.api.auth.integration.interfaces.IdentityProvider;
import io.cattle.platform.iaas.api.auth.integration.ldap.ad.LdapServiceContextPoolFactory;
import io.cattle.platform.iaas.api.auth.integration.ldap.interfaces.LDAPConstants;
import io.cattle.platform.pool.PoolConfig;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LDAPIdentityProvider implements IdentityProvider{

    private static final Logger log = LoggerFactory.getLogger(LDAPIdentityProvider.class);

    @Override
    public List<Identity> searchIdentities(String name, boolean exactMatch) {
        if (!isConfigured()){
            notConfigured();
        }
        List<Identity> identities = new ArrayList<>();
        for (String scope : scopes()) {
            identities.addAll(searchIdentities(name, scope, exactMatch));
        }
        return identities;
    }


    @Override
    public List<Identity> searchIdentities(String name, String scope, boolean exactMatch) {
        if (!isConfigured()){
            notConfigured();
        }
        name = escapeLDAPSearchFilter(name);
        if (getConstantsConfig().getUserScope().equalsIgnoreCase(scope)) {
            return searchUser(name, exactMatch);
        } else if(getConstantsConfig().getGroupScope().equalsIgnoreCase(scope)) {
            return searchGroup(name, exactMatch);
        } else{
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "invalidScope", "Identity type is not valid for Ldap", null);
        }
    }

    @Override
    public Identity getIdentity(String distinguishedName, String scope) {
        if (!isConfigured()){
            notConfigured();
        }
        if (!getConstantsConfig().scopes().contains(scope)) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "invalidScope", "Identity type is not valid for Ldap", null);
        }
        try {
            return getObject(distinguishedName, scope);
        }
        catch (ServiceContextCreationException e){
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "LdapDown", "Could not create service context.", null);
        } catch (ServiceContextRetrievalException e) {
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "LdapDown", "Could not retrieve service context.", null);
        }
    }

    @Override
    public Identity transform(Identity identity) {
        if (getConstantsConfig().scopes().contains(identity.getExternalIdType())) {
            return getIdentity(identity.getExternalId(), identity.getExternalIdType());
        } else {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST,
                    IdentityConstants.INVALID_TYPE, "Ldap does not provide: " + identity.getExternalIdType(), null);
        }
    }

    @Override
    public Identity untransform(Identity identity) {
        if (!getConstantsConfig().scopes().contains(identity.getExternalIdType())){
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST,
                    IdentityConstants.INVALID_TYPE, "Ldap does not provide: " + identity.getExternalIdType(), null);
        }
        return identity;
    }

    protected List<Identity> searchGroup(String name, boolean exactMatch) {
        String query;
        if (exactMatch) {
            query = "(&(" + getConstantsConfig().getGroupSearchField() + '=' + name + ")(" + getConstantsConfig().objectClass() + '='
                    + getConstantsConfig().getGroupObjectClass() + "))";
        } else {
            query = "(&(" + getConstantsConfig().getGroupSearchField() + "=*" + name + "*)(" + getConstantsConfig().objectClass() + '='
                    + getConstantsConfig().getGroupObjectClass() + "))";
        }
        log.trace("LDAPIdentityProvider searchGroup query: " + query);
        return resultsToIdentities(searchLdap(query));
    }

    protected List<Identity> searchUser(String name, boolean exactMatch) {
        String query;
        if (exactMatch)
        {
            query = "(&(" + getConstantsConfig().getUserSearchField() + '=' + name + ")(" + getConstantsConfig().objectClass() + '='
                    + getConstantsConfig().getUserObjectClass() + "))";
        } else {
            query = "(&(" + getConstantsConfig().getUserSearchField() + "=*" + name + "*)(" + getConstantsConfig().objectClass() + '='
                    + getConstantsConfig().getUserObjectClass() + "))";
        }
        log.trace("LDAPIdentityProvider searchUser query: " + query);
        return resultsToIdentities(searchLdap(query));
    }

    protected List<Identity> resultsToIdentities(NamingEnumeration<SearchResult> results) {
        List<Identity> identities = new ArrayList<>();
        try {
            if (!results.hasMore()) {
                return identities;
            }
        } catch (NamingException e) {
            return identities;
        }
        try {
            while (results.hasMore()){
                SearchResult result = results.next();
                log.trace("LDAPIdentityProvider SearchResult: " + result);
                LdapName dn = new LdapName(result.getNameInNamespace());
                Identity identityObj = attributesToIdentity(dn);
                if (identityObj != null) {
                    identities.add(identityObj);
                }
            }
        } catch (NamingException e) {
            //Ldap Referrals are causing this.
            getLogger().debug("While iterating results from an ldap search.", e);
            return identities;
        }
        return identities;
    }

    protected Identity getObject(String distinguishedName, String scope) {
        LdapContext context = null;
        try {
            LdapName object = new LdapName(distinguishedName);
            context = getServiceContext();
            Attributes search;
            search = context.getAttributes(object);
            if (!isType(search, scope) && !hasPermission(search)){
                return null;
            }
            return attributesToIdentity(object);
        }
        catch (NamingException e) {
            getLogger().info("Failed to get object: {} : {}", distinguishedName, e.getExplanation());
            return null;
        }
        finally {
            if (context != null) {
                getContextPool().returnObject(context);
            }
        }
    }

    protected Identity attributesToIdentity(LdapName dn){
        LdapContext context = getServiceContext();
        try {
            Attributes search = context.getAttributes(dn);
            log.trace("Attributes for dn: " + dn + " to translate: " + search);
            String externalIdType;
            String accountName;
            String externalId = dn.toString();
            String login;
            if (isType(search, getConstantsConfig().getUserObjectClass())){
                externalIdType = getConstantsConfig().getUserScope();
                if (search.get(getConstantsConfig().getUserNameField()) != null) {
                    accountName = (String) search.get(getConstantsConfig().getUserNameField()).get();
                } else {
                    accountName = externalId;
                }
                login = (String) search.get(getConstantsConfig().getUserLoginField()).get();
            } else if (isType(search, getConstantsConfig().getGroupObjectClass())) {
                externalIdType = getConstantsConfig().getGroupScope();
                if (search.get(getConstantsConfig().getGroupNameField()) != null) {
                    accountName = (String) search.get(getConstantsConfig().getGroupNameField()).get();
                } else {
                    accountName = externalId;
                }
                if (search.get(getConstantsConfig().getUserLoginField()) != null) {
                    login = (String) search.get(getConstantsConfig().getUserLoginField()).get();
                } else {
                    login = accountName;
                }
            } else {
                return null;
            }
            return new Identity(externalIdType, externalId, accountName, null, null, login);
        } catch (NamingException e) {
            return null;
        } finally {
            if (context != null) {
                getContextPool().returnObject(context);
            }
        }
    }

    protected boolean isType(Attributes search, String type) {
        NamingEnumeration<?> objectClass;
        try {
            objectClass = search.get(getConstantsConfig().objectClass()).getAll();
            while (objectClass.hasMoreElements()) {
                Object object = objectClass.next();
                if ((object.toString()).equalsIgnoreCase(type)){
                    return true;
                }
            }
            return false;
        } catch (NamingException e) {
            getLogger().info("Failed to determine if object is type:" + type, e);
            return false;
        }
    }

    protected NamingEnumeration<SearchResult> searchLdap(String query) {
        SearchControls controls = new SearchControls();
        LdapContext context = null;
        controls.setSearchScope(SUBTREE_SCOPE);
        NamingEnumeration<SearchResult> results;
        try {
            context = getServiceContext();
            results = context.search(getConstantsConfig().getDomain(), query, controls);
        } catch (NamingException e) {
            getLogger().error("When searching ldap from /v1/identity Failed to search: " + query + " scope:" + getConstantsConfig().getDomain(), e);
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, getConstantsConfig().getConfig(),
                    "Organizational Unit not found.", null);
        } finally {
            if (context != null){
                getContextPool().returnObject(context);
            }
        }
        return results;
    }

    protected boolean hasPermission(Attributes attributes){
        int permission;
        try {
            if (!isType(attributes, getConstantsConfig().getUserObjectClass())){
                return true;
            }
            if (StringUtils.isNotBlank(getConstantsConfig().getUserEnabledAttribute())) {
                permission = Integer.parseInt(attributes.get(getConstantsConfig().getUserEnabledAttribute()).get()
                        .toString());
            } else {
                return true;
            }
        } catch (NamingException e) {
            getLogger().error("Failed to get USER_ENABLED_ATTRIBUTE.", e);
            return false;
        }
        permission = permission & getConstantsConfig().getUserDisabledBitMask();
        return permission != getConstantsConfig().getUserDisabledBitMask();
    }

    protected LdapContext login(String username, String password) {
        if (StringUtils.isEmpty(password)) {
            throw new UserLoginFailureException("Failed to login ldap User : Invalid Credentials");
        }
        Hashtable<String, String> props = new Hashtable<>();
        props.put(Context.SECURITY_AUTHENTICATION, "simple");
        props.put(Context.SECURITY_PRINCIPAL, username);
        props.put(Context.SECURITY_CREDENTIALS, password);
        props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        LdapContext userContext;

        try {
            String url = "ldap://" + getConstantsConfig().getServer() + ':' + getConstantsConfig().getPort() + '/';
            props.put(Context.PROVIDER_URL, url);
            if (getConstantsConfig().getTls()) {
                props.put(Context.SECURITY_PROTOCOL, "ssl");
            }
            userContext = new InitialLdapContext(props, null);
            return userContext;
        } catch (NamingException e) {
            throw new UserLoginFailureException("Failed to login ldap User: " +  LDAPUtils.errorCodeToDescription(e), e, username);
        }
    }

    protected String escapeLDAPSearchFilter(String filter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < filter.length(); i++) {
            char curChar = filter.charAt(i);
            switch (curChar) {
                case '\\':
                    sb.append("\\5c");
                    break;
                case '*':
                    sb.append("\\2a");
                    break;
                case '(':
                    sb.append("\\28");
                    break;
                case ')':
                    sb.append("\\29");
                    break;
                case '\u0000':
                    sb.append("\\00");
                    break;
                default:
                    sb.append(curChar);
            }
        }
        return sb.toString();
    }


    protected LdapContext getServiceContext() {
        try {
            return getContextPool().borrowObject();
        } catch (ServiceContextCreationException e) {
            throw e;
        } catch (Exception e) {
            getLogger().error("Failed to get service context for ldap.", e);
            throw new ServiceContextRetrievalException("Unable to borrow a service context from context pool.", e);
        }
    }


    @PostConstruct
    public void init() {
        if (getContextPool() == null) {
            GenericObjectPoolConfig config = new GenericObjectPoolConfig();
            config.setTestOnBorrow(true);
            PoolConfig.setConfig(config, "ldap.context.pool", "ldap.context.pool.", "global.pool.");
            LdapServiceContextPoolFactory serviceContextPoolFactory = new LdapServiceContextPoolFactory(getConstantsConfig());
            setContextPool(new GenericObjectPool<>(serviceContextPoolFactory, config));
            AbandonedConfig abandonedConfig = new AbandonedConfig();
            abandonedConfig.setUseUsageTracking(true);
            abandonedConfig.setRemoveAbandonedOnMaintenance(true);
            abandonedConfig.setRemoveAbandonedOnBorrow(true);
            abandonedConfig.setRemoveAbandonedTimeout(60);
            getContextPool().setAbandonedConfig(abandonedConfig);
        }
    }

    public void reset() {
        if (getContextPool() != null) {
            getContextPool().close();
            setContextPool(null);
        }
        init();
    }
    protected abstract void setContextPool(GenericObjectPool<LdapContext> ldapContextGenericObjectPool);

    protected abstract AbstractTokenUtil getTokenUtils();

    protected abstract GenericObjectPool<LdapContext> getContextPool();

    protected abstract LDAPConstants getConstantsConfig();

    protected abstract Logger getLogger();

    protected void notConfigured() {
        throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE,
                "NotConfigured", "Ldap is not configured", null);
    }

    public List<Identity> getIdentities(List<Map<String, String>> identitiesGiven) {
        if (identitiesGiven == null || identitiesGiven.isEmpty()){
            return new ArrayList<>();
        }

        List<Identity> identities = new ArrayList<>();
        for (Map<String, String> identity: identitiesGiven){
            String externalId = identity.get(IdentityConstants.EXTERNAL_ID);
            String externalIdType = identity.get(IdentityConstants.EXTERNAL_ID_TYPE);
            Identity gotIdentity = getIdentity(externalId, externalIdType);
            if (gotIdentity == null) {
                throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "InvalidIdentity", "Invalid Identity", null);
            }
            identities.add(gotIdentity);
        }
        return identities;
    }
}
