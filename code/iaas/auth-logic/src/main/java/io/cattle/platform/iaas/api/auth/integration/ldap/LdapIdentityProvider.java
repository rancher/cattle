package io.cattle.platform.iaas.api.auth.integration.ldap;

import static javax.naming.directory.SearchControls.*;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.constants.IdentityConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.AuthToken;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.dao.AuthTokenDao;
import io.cattle.platform.iaas.api.auth.identity.Token;
import io.cattle.platform.iaas.api.auth.integration.interfaces.IdentityProvider;
import io.cattle.platform.pool.PoolConfig;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public class LdapIdentityProvider extends LdapConfigurable implements IdentityProvider {

    private static final Log logger = LogFactory.getLog(LdapIdentityProvider.class);
    @Inject
    LdapTokenUtils ldapTokenUtils;
    @Inject
    LdapTokenCreator ldapTokenCreator;
    @Inject
    AuthTokenDao authTokenDao;
    GenericObjectPool<LdapContext> contextPool;
    ExecutorService executorService;

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
        name = escapeLDAPSearchFilter(name);
        switch (scope) {
            case LdapConstants.USER_SCOPE:
                return searchUser(name, exactMatch);
            case LdapConstants.GROUP_SCOPE:
                return searchGroup(name, exactMatch);
            default:
                throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "invalidScope", "Identity type is not valid for Ldap", null);
        }
    }

    @Override
    public Set<Identity> getIdentities(Account account) {
        if (!isConfigured() || !LdapConstants.USER_SCOPE.equalsIgnoreCase(account.getExternalIdType())) {
            return new HashSet<>();
        }
        if(!ldapTokenUtils.findAndSetJWT() &&
                SecurityConstants.SECURITY.get() &&
                LdapConstants.CONFIG.equalsIgnoreCase(SecurityConstants.AUTH_PROVIDER.get())) {
            AuthToken authToken = authTokenDao.getTokenByAccountId(account.getId());
            if (authToken == null){
                String query = "(" + LdapConstants.DN + '=' + account.getExternalId() + ")";
                SearchResult userRecord = userRecord(query);

                if (userRecord == null){
                    return new HashSet<>();
                }
                Set<Identity> identities = getIdentities(userRecord);
                Token token = ldapTokenUtils.createToken(identities, null);
                authToken = authTokenDao.createToken(token.getJwt(), LdapConstants.CONFIG, account.getId());
            }
            if (authToken != null && authToken.getKey() != null) {
                ApiRequest request = ApiContext.getContext().getApiRequest();
                request.setAttribute(LdapConstants.LDAP_JWT, authToken.getKey());
            }
        }
        return ldapTokenUtils.getIdentities();
    }

    @Override
    public Identity getIdentity(String distinguishedName, String scope) {
        if (!isConfigured()){
            notConfigured();
        }
        switch (scope) {
            case LdapConstants.USER_SCOPE:
            case LdapConstants.GROUP_SCOPE:
                try {
                    return getObject(distinguishedName, scope);
                }
                catch (ServiceContextCreationException e){
                    throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "LdapDown", "Could not create service context.", null);
                } catch (ServiceContextRetrievalException e) {
                    throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "LdapDown", "Could not retrieve service context.", null);
                }
            default:
                throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "invalidScope", "Identity type is not valid for Ldap", null);
        }
    }

    @Override
    public Set<String> scopes() {
        return LdapConstants.SCOPES;
    }

    @Override
    public String getName() {
        return LdapConstants.NAME;
    }

    LdapContext login(String username, String password) {
        Hashtable<String, String> props = new Hashtable<>();
        props.put(Context.SECURITY_AUTHENTICATION, "simple");
        props.put(Context.SECURITY_PRINCIPAL, username);
        props.put(Context.SECURITY_CREDENTIALS, password);
        props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        LdapContext userContext;

        try {
            String url = "ldap://" + LdapConstants.LDAP_SERVER.get() + ':' + LdapConstants.LDAP_PORT.get() + '/';
            props.put(Context.PROVIDER_URL, url);
            if (LdapConstants.TLS_ENABLED.get()) {
                props.put(Context.SECURITY_PROTOCOL, "ssl");
            }
            userContext = new InitialLdapContext(props, null);
            return userContext;
        } catch (NamingException e) {
            throw new UserLoginFailureException("Failed to login ldap User.", e, username);
        }
    }

    private SearchResult userRecord(String query) {
        LdapContext context = getServiceContext();
        try {
            SearchControls controls = new SearchControls();
            controls.setSearchScope(SUBTREE_SCOPE);
            NamingEnumeration<SearchResult> results;
            try {
                results = context.search(LdapConstants.LDAP_DOMAIN.get(), query, controls);
            } catch (NamingException e) {
                logger.info("Failed to search: " + query + "with DN=" + LdapConstants.LDAP_DOMAIN.get() + " as the scope.", e);
                throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "LdapConfig",
                        "Organizational Unit not found.", null);
            }
            try {
                if (!results.hasMore()) {
                    logger.error("Cannot locate user information for " + query);
                    return null;
                }
            } catch (NamingException e) {
                logger.error(query + " is not found.", e);
                return null;
            }
            SearchResult result;
            try {
                result = results.next();
                if (results.hasMoreElements()) {
                    logger.error("More than one result.");
                    return null;
                }
                if (!hasPermission(result.getAttributes())) {
                    return null;
                }
            } catch (NamingException e) {
                logger.error("No results. when searching. " + query);
                return null;
            }
            return result;
        } finally {
            if (context != null) {
                contextPool.returnObject(context);
            }
        }
    }

    private Set<Identity> getIdentities(SearchResult result) {
        final Set<Identity> identities = new HashSet<>();
        final Attribute memberOf = result.getAttributes().get(LdapConstants.MEMBER_OF);
        try {
            if (!isType(result.getAttributes(), LdapConstants.USER_OBJECT_CLASS.get()))
            {
                return identities;
            }
            Collection<Callable<Identity>> groupsToGet = new ArrayList<>();
            Identity user = attributesToIdentity(result.getNameInNamespace(), result.getAttributes());
            identities.add(user);
            if (memberOf != null) {// null if this user belongs to no group at all
                for (int i = 0; i < memberOf.size(); i++) {
                    final int finalI = i;
                    groupsToGet.add(new Callable<Identity>() {
                        @Override
                        public Identity call() throws Exception {
                            try {
                                Identity identity;
                                try {
                                    identity = getObject(memberOf.get(finalI).toString(), LdapConstants.GROUP_SCOPE);
                                }  catch (ServiceContextRetrievalException | ServiceContextCreationException e){
                                    logger.error("Ldap Failure during group retrieval: " + e.getMessage(), e);
                                    throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "LdapDown",
                                            e.getMessage(), null);
                                }
                                identities.add(identity);
                                return identity;
                            } catch (NamingException e) {
                                logger.error("Failed to get a group", e);
                                return null;
                            }
                        }
                    });
                }
            }
            try {
                executorService.invokeAll(groupsToGet);
            } catch (InterruptedException e) {
                logger.error("Interrupted when getting groups from ldap.", e);
                throw new RuntimeException(e);
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
        try {
            LdapContext userContext= login(getUserExternalId(username), password);
            try {
                if (userContext != null){
                    userContext.close();
                }
            } catch (NamingException e) {
                logger.error("Failed to close userContext. Reason: " + e.getExplanation());
            }
        } catch (UserLoginFailureException e) {
            logger.info("Failed to login to ldap user:" + username + " " + e.getUsername() + "Original cause: " + e.getMessage());
            throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
        }
        String name = getSamName(username);
        String query = "(" + LdapConstants.USER_LOGIN_FIELD.get() + '=' + name + ")";
        SearchResult userRecord = userRecord(query);
        if (userRecord == null){
            return new HashSet<>();
        }
        return getIdentities(userRecord);
    }

    public String getUserExternalId(String username) {
        if (!isConfigured()) {
            notConfigured();
        }
        if (username.contains("\\")) {
            return username;
        } else if (StringUtils.isNotBlank(LdapConstants.LDAP_LOGIN_DOMAIN.get())) {
            return LdapConstants.LDAP_LOGIN_DOMAIN.get() + '\\' +username;
        } else {
            return username;
        }
    }

    public String getSamName(String username) {
        if (!isConfigured()) {
            notConfigured();
        }
        if (username.contains("\\")) {
            return username.split("\\\\", 2)[1];
        } else {
            return username;
        }
    }

    private List<Identity> searchGroup(String name, boolean exactMatch) {
        String query;
        if (exactMatch) {
            query = "(&(" + LdapConstants.GROUP_SEARCH_FIELD.get() + '=' + name + ")(" + LdapConstants.OBJECT_CLASS + '='
                    + LdapConstants.GROUP_OBJECT_CLASS.get() + "))";
        } else {
            query = "(&(" + LdapConstants.GROUP_SEARCH_FIELD.get() + "=*" + name + "*)(" + LdapConstants.OBJECT_CLASS + '='
                    + LdapConstants.GROUP_OBJECT_CLASS.get() + "))";
        }
        return resultsToIdentities(searchLdap(query));
    }

    private List<Identity> searchUser(String name, boolean exactMatch) {
        String query;
        if (exactMatch)
        {
            query = "(&(" + LdapConstants.USER_SEARCH_FIELD.get() + '=' + name + ")(" + LdapConstants.OBJECT_CLASS + '='
                    + LdapConstants.USER_OBJECT_CLASS.get() + "))";
        } else {
            query = "(&(" + LdapConstants.USER_SEARCH_FIELD.get() + "=*" + name + "*)(" + LdapConstants.OBJECT_CLASS + '='
                    + LdapConstants.USER_OBJECT_CLASS.get() + "))";
        }
        return resultsToIdentities(searchLdap(query));
    }

    private Identity getObject(String distinguishedName, String scope) {
        LdapContext context = null;
        try {
            context = getServiceContext();
            Attributes search;
            search = context.getAttributes(new LdapName(distinguishedName));
            if (!isType(search, scope) && !hasPermission(search)){
                return null;
            }
            return attributesToIdentity(distinguishedName, search);
        }
        catch (NamingException e) {
            logger.info("Failed to get object: " + distinguishedName, e);
            return null;
        }
        finally {
            if (context != null) {
                contextPool.returnObject(context);
            }
        }
    }

    private boolean isType(Attributes search, String type) throws NamingException {
        NamingEnumeration<?> objectClass = search.get(LdapConstants.OBJECT_CLASS).getAll();
        boolean isType = false;
        while (objectClass.hasMoreElements()) {
            Object object = objectClass.next();
            if ((object.toString()).equalsIgnoreCase(type)){
                isType = true;
            }
        }
        return isType;
    }

    private NamingEnumeration<SearchResult> searchLdap(String query) {
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SUBTREE_SCOPE);
        NamingEnumeration<SearchResult> results;
        try {
            results = getServiceContext().search(LdapConstants.LDAP_DOMAIN.get(), query, controls);
        } catch (NamingException e) {
            logger.error("When searching ldap from /v1/identity Failed to search: " + query + " scope:" + LdapConstants.LDAP_DOMAIN.get(), e);
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, LdapConstants.CONFIG,
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
            while (results.hasMore()) {
                SearchResult result = results.next();
                identities.add(attributesToIdentity(result.getNameInNamespace(), result.getAttributes()));
        }
        } catch (NamingException e) {
            //Ldap Referrals are causing this.
            logger.debug("While iterating results while searching errored.", e);
            return identities;
        }
        return identities;
    }

    private Identity attributesToIdentity(String distinguishedName, Attributes search){
        try {
            String externalIdType;
            String accountName;
            String login;
            if (isType(search, LdapConstants.USER_OBJECT_CLASS.get())){
                externalIdType = LdapConstants.USER_SCOPE;
                if (search.get(LdapConstants.USER_NAME_FIELD.get()) != null) {
                    accountName = (String) search.get(LdapConstants.USER_NAME_FIELD.get()).get();
                } else {
                    accountName = distinguishedName;
                }
                login = (String) search.get(LdapConstants.USER_LOGIN_FIELD.get()).get();
            } else if (isType(search, LdapConstants.GROUP_OBJECT_CLASS.get())) {
                externalIdType = LdapConstants.GROUP_SCOPE;
                if (search.get(LdapConstants.GROUP_NAME_FIELD.get()) != null) {
                    accountName = (String) search.get(LdapConstants.GROUP_NAME_FIELD.get()).get();
                } else {
                    accountName = distinguishedName;
                }
                if (search.get(LdapConstants.USER_LOGIN_FIELD.get()) != null) {
                    login = (String) search.get(LdapConstants.USER_LOGIN_FIELD.get()).get();
                } else {
                    login = accountName;
                }
            } else {
                return null;
            }
            return new Identity(externalIdType, distinguishedName, accountName, null, null, login);
        } catch (NamingException e) {
            return null;
        }
    }

    private boolean hasPermission(Attributes attributes){
        int permission;
        try {
            if (!isType(attributes, LdapConstants.USER_OBJECT_CLASS.get())){
                return true;
            }
            if (StringUtils.isNotBlank(LdapConstants.USER_ENABLED_ATTRIBUTE.get())) {
                permission = Integer.parseInt(attributes.get(LdapConstants.USER_ENABLED_ATTRIBUTE.get()).get()
                        .toString());
            } else {
                return true;
            }
        } catch (NamingException e) {
            logger.error("Failed to get USER_ENABLED_ATTRIBUTE.", e);
            return false;
        }
        permission = permission & LdapConstants.USER_DISABLED_BIT_MASK.get();
        return permission != LdapConstants.USER_DISABLED_BIT_MASK.get();
    }

    private String escapeLDAPSearchFilter(String filter) {
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

    @Override
    public Identity transform(Identity identity) {
        switch (identity.getExternalIdType()) {
            case LdapConstants.USER_SCOPE:
            case LdapConstants.GROUP_SCOPE:
                return getIdentity(identity.getExternalId(), identity.getExternalIdType());
            default:
                throw new ClientVisibleException(ResponseCodes.BAD_REQUEST,
                        IdentityConstants.INVALID_TYPE, "Ldap does not provide: " + identity.getExternalIdType(), null);
        }
    }

    @Override
    public Identity untransform(Identity identity) {
        switch (identity.getExternalIdType()) {
            case LdapConstants.USER_SCOPE:
                break;
            case LdapConstants.GROUP_SCOPE:
                break;
            default:
                throw new ClientVisibleException(ResponseCodes.BAD_REQUEST,
                        IdentityConstants.INVALID_TYPE, "Ldap does not provide: " + identity.getExternalIdType(), null);
        }
        return identity;
    }

    @PostConstruct
    public void init() {
        if (contextPool == null) {
            GenericObjectPoolConfig config = new GenericObjectPoolConfig();
            PoolConfig.setConfig(config, "ldap.context.pool", "ldap.context.pool.", "global.pool.");
            contextPool = new GenericObjectPool<>(new LdapServiceContextPoolFactory(), config);
        }
    }

    private LdapContext getServiceContext() {
        try {
            return contextPool.borrowObject();
        } catch (ServiceContextCreationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to get service context for ldap.", e);
            throw new ServiceContextRetrievalException("Unable to borrow a service context from context pool.", e);
        }
    }

    @Inject
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }
}
