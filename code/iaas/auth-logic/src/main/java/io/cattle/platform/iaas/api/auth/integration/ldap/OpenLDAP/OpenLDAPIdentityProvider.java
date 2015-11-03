package io.cattle.platform.iaas.api.auth.integration.ldap.OpenLDAP;

import static javax.naming.directory.SearchControls.*;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.constants.IdentityConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.AuthToken;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.dao.AuthTokenDao;
import io.cattle.platform.iaas.api.auth.identity.Token;
import io.cattle.platform.iaas.api.auth.integration.interfaces.IdentityProvider;
import io.cattle.platform.iaas.api.auth.integration.ldap.ad.ADIdentityProvider;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class OpenLDAPIdentityProvider extends OpenLDAPConfigurable implements IdentityProvider {

    private static final Log logger = LogFactory.getLog(OpenLDAPIdentityProvider.class);
    @Inject
    OpenLDAPUtils OpenLDAPUtils;
    @Inject
    OpenLDAPTokenCreator ADTokenCreator;
    @Inject
    AuthTokenDao authTokenDao;

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

    private void notConfigured() {
        throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE,
                "NotConfigured", "Ldap is not configured", null);
    }

    @Override
    public List<Identity> searchIdentities(String name, String scope, boolean exactMatch) {
        if (!isConfigured()){
            notConfigured();
        }
        name = ADIdentityProvider.escapeLDAPSearchFilter(name);
        switch (scope) {
            case OpenLDAPConstants.USER_SCOPE:
                return searchUser(name, exactMatch);
            case OpenLDAPConstants.GROUP_SCOPE:
                return searchGroup(name, exactMatch);
            default:
                throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "invalidScope", "Identity type is not valid for Ldap", null);
        }
    }

    @Override
    public Set<Identity> getIdentities(Account account) {
        if (!isConfigured() || !OpenLDAPConstants.USER_SCOPE.equalsIgnoreCase(account.getExternalIdType())) {
            return new HashSet<>();
        }
        if(!OpenLDAPUtils.findAndSetJWT() &&
                SecurityConstants.SECURITY.get() &&
                OpenLDAPConstants.CONFIG.equalsIgnoreCase(SecurityConstants.AUTH_PROVIDER.get())) {
            AuthToken authToken = authTokenDao.getTokenByAccountId(account.getId());
            if (authToken == null){
                LdapName dn;
                try {
                    dn = new LdapName(account.getExternalId());
                } catch (NamingException e) {
                    throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
                }
                Set<Identity> identities = getIdentities(dn);
                Token token = OpenLDAPUtils.createToken(identities, null);
                authToken = authTokenDao.createToken(token.getJwt(), OpenLDAPConstants.CONFIG, account.getId());
            }
            if (authToken != null && authToken.getKey() != null) {
                ApiRequest request = ApiContext.getContext().getApiRequest();
                request.setAttribute(OpenLDAPConstants.LDAP_JWT, authToken.getKey());
            }
        }
        return OpenLDAPUtils.getIdentities();
    }

    @Override
    public Identity getIdentity(String distinguishedName, String scope) {
        if (!isConfigured()){
            notConfigured();
        }
        switch (scope) {
            case OpenLDAPConstants.USER_SCOPE:
            case OpenLDAPConstants.GROUP_SCOPE:
                return getObject(distinguishedName, scope);
            default:
                throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "invalidScope", "Identity type is not valid for Ldap", null);
        }
    }

    @Override
    public Set<String> scopes() {
        return OpenLDAPConstants.SCOPES;
    }

    @Override
    public String getName() {
        return OpenLDAPConstants.NAME;
    }

    private boolean login(String username, String password) {
        Hashtable<String, String> props = new Hashtable<>();
        props.put(Context.SECURITY_AUTHENTICATION, "simple");
        props.put(Context.SECURITY_PRINCIPAL, username);
        props.put(Context.SECURITY_CREDENTIALS, password);
        props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        try {
            String url = "ldap://" + OpenLDAPConstants.LDAP_SERVER.get() + ':' + OpenLDAPConstants.LDAP_PORT.get() + '/';
            props.put(Context.PROVIDER_URL, url);
            if (OpenLDAPConstants.TLS_ENABLED.get()) {
                props.put(Context.SECURITY_PROTOCOL, "ssl");
            }
            new InitialLdapContext(props, null);
            return true;
        } catch (NamingException e) {
            logger.info("Failed to login to ldap user:" + username);
            return false;
        }
    }

    private Set<Identity> getIdentities(LdapName dn) {
        Set<Identity> identities = new HashSet<>();
        Attributes userAttributes;
        try {
            userAttributes = getServiceContext().getAttributes(dn);
            if (!hasPermission(userAttributes)){
                throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
            }
        } catch (NamingException e) {
            throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
        }
        Attribute memberOf = userAttributes.get(OpenLDAPConstants.USER_MEMBER_ATTRIBUTE.get());
        try {
            if (!isType(userAttributes, OpenLDAPConstants.USER_OBJECT_CLASS.get()))
            {
                return identities;
            }
            Identity user = attributesToIdentity(dn);
            identities.add(user);
            if (memberOf != null) {// null if this user belongs to no group at all
                for (int i = 0; i < memberOf.size(); i++) {
                    identities.addAll(resultsToIdentities(searchLdap("(&(" + OpenLDAPConstants.USER_MEMBER_ATTRIBUTE
                            .get() +
                            '=' + memberOf.get(i).toString() + ")(" + OpenLDAPConstants.OBJECT_CLASS + '=' +
                            OpenLDAPConstants.GROUP_OBJECT_CLASS.get() + "))")));

                }
            }
            if (user != null && StringUtils.isNotBlank(user.getLogin())){
                String query = "(&(" + OpenLDAPConstants.GROUP_MEMBER_MAPPING_ATTRIBUTE.get()
                        + '=' + user.getLogin()
                        + ")(" + OpenLDAPConstants.OBJECT_CLASS + '='
                        + OpenLDAPConstants.GROUP_OBJECT_CLASS.get() + "))";
                identities.addAll(resultsToIdentities(searchLdap(query)));
            }
            return identities;
        } catch (NamingException e) {
            logger.error("Exceptions on groups.", e);
            return new HashSet<>();
        }
    }

    public Set<Identity> getIdentities(String username, String password) {
        if (!isConfigured()) {
            return new HashSet<>();
        }
        LdapName user;
        List<Identity> users = searchUser(username, true);
        if (users.size() != 1){
            logger.error("Found no or multiple users for: " + username);
            throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
        } else {
            try {
                if (login(users.get(0).getExternalId(), password)){
                    user = new LdapName(users.get(0).getExternalId());
                } else {
                    throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
                }
            } catch (NamingException e) {
                logger.error("Somehow got bad Distinguished name from api.");
                throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR);
            }
        }
        return getIdentities(user);
    }

    private List<Identity> searchGroup(String name, boolean exactMatch) {
        String query;
        if (exactMatch) {
            query = "(&(" + OpenLDAPConstants.GROUP_SEARCH_FIELD.get() + '=' + name + ")(" + OpenLDAPConstants.OBJECT_CLASS + '='
                    + OpenLDAPConstants.GROUP_OBJECT_CLASS.get() + "))";
        } else {
            query = "(&(" + OpenLDAPConstants.GROUP_SEARCH_FIELD.get() + "=*" + name + "*)(" + OpenLDAPConstants.OBJECT_CLASS + '='
                    + OpenLDAPConstants.GROUP_OBJECT_CLASS.get() + "))";
        }
        return resultsToIdentities(searchLdap(query));
    }

    private List<Identity> searchUser(String name, boolean exactMatch) {
        String query;
        if (exactMatch)
        {
            query = "(&(" + OpenLDAPConstants.USER_SEARCH_FIELD.get() + '=' + name + ")(" + OpenLDAPConstants.OBJECT_CLASS + '='
                    + OpenLDAPConstants.USER_OBJECT_CLASS.get() + "))";
        } else {
            query = "(&(" + OpenLDAPConstants.USER_SEARCH_FIELD.get() + "=*" + name + "*)(" + OpenLDAPConstants.OBJECT_CLASS + '='
                    + OpenLDAPConstants.USER_OBJECT_CLASS.get() + "))";
        }
        return resultsToIdentities(searchLdap(query));
    }

    private Identity getObject(String distinguishedName, String scope) {
        try {
            LdapName object = new LdapName(distinguishedName);
            Attributes search = getServiceContext().getAttributes(object);
            if (!isType(search, scope) && !hasPermission(search)){
                return null;
            }
            return attributesToIdentity(object);
        } catch (NamingException e) {
            return null;
        }
    }

    private boolean isType(Attributes search, String type) {
        NamingEnumeration<?> objectClass;
        try {
            objectClass = search.get(OpenLDAPConstants.OBJECT_CLASS).getAll();
            while (objectClass.hasMoreElements()) {
                Object object = objectClass.next();
                if ((object.toString()).equalsIgnoreCase(type)){
                    return true;
                }
            }
            return false;
        } catch (NamingException e) {
            logger.info("Failed to determine if object is type:" + type, e);
            return false;
        }
    }

    private LdapContext getServiceContext() {
        Hashtable<String, String> props = new Hashtable<>();
        props.put(Context.SECURITY_AUTHENTICATION, "simple");
        props.put(Context.SECURITY_PRINCIPAL, OpenLDAPConstants.SERVICE_ACCOUNT_USER.get());
        props.put(Context.SECURITY_CREDENTIALS, OpenLDAPConstants.SERVICE_ACCOUNT_PASSWORD.get());
        props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        LdapContext userContext;
        try {
            String url = "ldap://" + OpenLDAPConstants.LDAP_SERVER.get() + ':' + OpenLDAPConstants.LDAP_PORT.get() + '/';
            props.put(Context.PROVIDER_URL, url);
            if (OpenLDAPConstants.TLS_ENABLED.get()) {
                props.put(Context.SECURITY_PROTOCOL, "ssl");
            }
            userContext = new InitialLdapContext(props, null);
            return userContext;
        } catch (NamingException e) {
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "CattleServiceAccount",
                    "Could not connect to LDAP with service account, contact admin of Rancher or LDAP", null);
        }
    }

    private NamingEnumeration<SearchResult> searchLdap(String query) {
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SUBTREE_SCOPE);
        NamingEnumeration<SearchResult> results;
        try {
            results = getServiceContext().search(OpenLDAPConstants.LDAP_DOMAIN.get(), query, controls);
        } catch (NamingException e) {
            logger.error("When searching ldap from /v1/identity Failed to search: " + query + " scope:" + OpenLDAPConstants.LDAP_DOMAIN.get(), e);
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "OpenLDAPConfig",
                    "Organizational Unit not found.", null);
        }
        return results;
    }

    private List<Identity> resultsToIdentities(NamingEnumeration<SearchResult> results) {
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
                LdapName dn = new LdapName(result.getNameInNamespace());
                identities.add(attributesToIdentity(dn));
            }
        } catch (NamingException e) {
            //Ldap Referrals are causing this.
            logger.debug("While iterating results from an ldap search.", e);
            return identities;
        }
        return identities;
    }

    private Identity attributesToIdentity(LdapName dn){
        try {
            Attributes search = getServiceContext().getAttributes(dn);
            String externalIdType;
            String accountName;
            String externalId = dn.toString();
            String login;
            if (isType(search, OpenLDAPConstants.USER_OBJECT_CLASS.get())){
                externalIdType = OpenLDAPConstants.USER_SCOPE;
                if (search.get(OpenLDAPConstants.USER_NAME_FIELD.get()) != null) {
                    accountName = (String) search.get(OpenLDAPConstants.USER_NAME_FIELD.get()).get();
                } else {
                    accountName = externalId;
                }
                login = (String) search.get(OpenLDAPConstants.USER_LOGIN_FIELD.get()).get();
            } else if (isType(search, OpenLDAPConstants.GROUP_OBJECT_CLASS.get())) {
                externalIdType = OpenLDAPConstants.GROUP_SCOPE;
                if (search.get(OpenLDAPConstants.GROUP_NAME_FIELD.get()) != null) {
                    accountName = (String) search.get(OpenLDAPConstants.GROUP_NAME_FIELD.get()).get();
                } else {
                    accountName = externalId;
                }
                if (search.get(OpenLDAPConstants.USER_LOGIN_FIELD.get()) != null) {
                    login = (String) search.get(OpenLDAPConstants.USER_LOGIN_FIELD.get()).get();
                } else {
                    login = accountName;
                }
            } else {
                return null;
            }
            return new Identity(externalIdType, externalId, accountName, null, null, login);
        } catch (NamingException e) {
            return null;
        }
    }

    private boolean hasPermission(Attributes attributes){
        int permission;
        try {
            if (!isType(attributes, OpenLDAPConstants.USER_OBJECT_CLASS.get())){
                return true;
            }
            if (StringUtils.isNotBlank(OpenLDAPConstants.USER_ENABLED_ATTRIBUTE.get())) {
                permission = Integer.parseInt(attributes.get(OpenLDAPConstants.USER_ENABLED_ATTRIBUTE.get()).get()
                        .toString());
            } else {
                return true;
            }
        } catch (NamingException e) {
            logger.error("Failed to get USER_ENABLED_ATTRIBUTE.", e);
            return false;
        }
        permission = permission & OpenLDAPConstants.USER_DISABLED_BIT_MASK.get();
        return permission != OpenLDAPConstants.USER_DISABLED_BIT_MASK.get();
    }

    @Override
    public Identity transform(Identity identity) {
        switch (identity.getExternalIdType()) {
            case OpenLDAPConstants.USER_SCOPE:
            case OpenLDAPConstants.GROUP_SCOPE:
                return getIdentity(identity.getExternalId(), identity.getExternalIdType());
            default:
                throw new ClientVisibleException(ResponseCodes.BAD_REQUEST,
                        IdentityConstants.INVALID_TYPE, "Ldap does not provide: " + identity.getExternalIdType(), null);
        }
    }

    @Override
    public Identity untransform(Identity identity) {
        switch (identity.getExternalIdType()) {
            case OpenLDAPConstants.USER_SCOPE:
                break;
            case OpenLDAPConstants.GROUP_SCOPE:
                break;
            default:
                throw new ClientVisibleException(ResponseCodes.BAD_REQUEST,
                        IdentityConstants.INVALID_TYPE, "Ldap does not provide: " + identity.getExternalIdType(), null);
        }
        return identity;
    }
}
