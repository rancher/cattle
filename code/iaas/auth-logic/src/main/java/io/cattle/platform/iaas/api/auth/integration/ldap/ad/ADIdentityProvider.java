package io.cattle.platform.iaas.api.auth.integration.ldap.ad;

import static javax.naming.directory.SearchControls.*;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.AuthToken;
import io.cattle.platform.iaas.api.auth.AbstractTokenUtil;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.dao.AuthTokenDao;
import io.cattle.platform.iaas.api.auth.identity.Token;
import io.cattle.platform.iaas.api.auth.integration.interfaces.IdentityProvider;
import io.cattle.platform.iaas.api.auth.integration.ldap.LDAPIdentityProvider;
import io.cattle.platform.iaas.api.auth.integration.ldap.ServiceContextCreationException;
import io.cattle.platform.iaas.api.auth.integration.ldap.ServiceContextRetrievalException;
import io.cattle.platform.iaas.api.auth.integration.ldap.UserLoginFailureException;
import io.cattle.platform.iaas.api.auth.integration.ldap.interfaces.LDAPConstants;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ADIdentityProvider extends LDAPIdentityProvider implements IdentityProvider {

    private static final Logger logger = LoggerFactory.getLogger(ADIdentityProvider.class);
    @Inject
    ADTokenUtils adTokenUtils;
    GenericObjectPool<LdapContext> contextPool;
    ExecutorService executorService;
    @Inject
    ADConstantsConfig adConfig;
    @Inject
    AuthTokenDao authTokenDao;

    @Override
    public Set<Identity> getIdentities(Account account) {
        if (!isConfigured() || !getConstantsConfig().getUserScope().equalsIgnoreCase(account.getExternalIdType())) {
            return new HashSet<>();
        }
        if(!getTokenUtils().findAndSetJWT() &&
                SecurityConstants.SECURITY.get() &&
                getConstantsConfig().getConfig().equalsIgnoreCase(SecurityConstants.AUTH_PROVIDER.get())) {
            AuthToken authToken = authTokenDao.getTokenByAccountId(account.getId());
            if (authToken == null){
                String query = "(" + "distinguishedname" + '=' + account.getExternalId() + ")";
                SearchResult userRecord = userRecord(query);
                if (userRecord == null){
                    return new HashSet<>();
                }
                Set<Identity> identities = getIdentities(userRecord);

                Token token = getTokenUtils().createToken(identities, null);
                authToken = authTokenDao.createToken(token.getJwt(), getConstantsConfig().getConfig(), account.getId());
            }
            if (authToken != null && authToken.getKey() != null) {
                ApiRequest request = ApiContext.getContext().getApiRequest();
                request.setAttribute(getConstantsConfig().getJWTType(), authToken.getKey());
            }
        }
        return getTokenUtils().getIdentities();
    }

    private SearchResult userRecord(String query) {
        LdapContext context = getServiceContext();
        try {
            SearchControls controls = new SearchControls();
            controls.setSearchScope(SUBTREE_SCOPE);
            NamingEnumeration<SearchResult> results;
            try {
                results = context.search(getConstantsConfig().getDomain(), query, controls);
            } catch (NamingException e) {
                logger.info("Failed to search: " + query + "with DN=" + getConstantsConfig().getDomain() + " as the scope.", e);
                throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "LdapConfig",
                        "Organizational Unit not found.", null);
            }

            if (!results.hasMoreElements()) {
                logger.error("Cannot locate user information for " + query);
                return null;
            }

            SearchResult result;
            try {
                result = results.next();
                logger.trace("Found  LDAP SearchResult object: {}", result);
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
        final Attribute memberOf = result.getAttributes().get(getConstantsConfig().getUserMemberAttribute());
        logger.trace("ADConstants userMemberAttribute() {}", getConstantsConfig().getUserMemberAttribute());
        logger.trace("SearchResult memberOf attribute {}", memberOf);
        if (!isType(result.getAttributes(), getConstantsConfig().getUserObjectClass()))
        {
            return identities;
        }
        Collection<Callable<Identity>> groupsToGet = new ArrayList<>();
        LdapName userName;
        try {
            userName = new LdapName(result.getNameInNamespace());
        } catch (InvalidNameException e) {
            getLogger().error("Got a result with an invalid ldap name.", e);
            throw new UserLoginFailureException(e);
        }
        Identity user = attributesToIdentity(userName);
        if (user != null) {
            identities.add(user);
        }
        if (memberOf != null) {// null if this user belongs to no group at all
            for (int i = 0; i < memberOf.size(); i++) {
                final int finalI = i;
                groupsToGet.add(new Callable<Identity>() {
                    @Override
                    public Identity call() throws Exception {
                        try {
                            Identity identity;
                            try {
                                identity = getObject(memberOf.get(finalI).toString(), getConstantsConfig().getGroupScope());
                            }  catch (ServiceContextRetrievalException | ServiceContextCreationException e){
                                logger.error("Ldap Failure during group retrieval: " + e.getMessage(), e);
                                throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "LdapDown",
                                        e.getMessage(), null);
                            }
                            if (identity != null) {
                                identities.add(identity);
                            }
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
        String query = "(" + getConstantsConfig().getUserLoginField() + '=' + name + ")";
        //if required access
        if(AbstractTokenUtil.isRequiredAccess(adTokenUtils.accessMode())) {
            String groupFilter = getAllowedIdentitiesFilter();
            if(groupFilter.length() > 1) {
                StringBuilder groupQuery = new StringBuilder("(&");
                groupQuery.append(query);
                groupQuery.append(groupFilter);
                groupQuery.append(")");
                query = groupQuery.toString();
            }
        }
        logger.trace("LDAP Search query: {}", query);
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
        } else if (StringUtils.isNotBlank(getConstantsConfig().getLoginDomain())) {
            return getConstantsConfig().getLoginDomain() + '\\' +username;
        } else {
            return username;
        }
    }

    private String getSamName(String username) {
        if (!isConfigured()) {
            notConfigured();
        }
        if (username.contains("\\")) {
            return username.split("\\\\", 2)[1];
        } else {
            return username;
        }
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    @Override
    protected GenericObjectPool<LdapContext> getContextPool() {
        return contextPool;
    }

    @Override
    public void setContextPool(GenericObjectPool<LdapContext> contextPool) {
        this.contextPool = contextPool;
    }

    @Override
    public LDAPConstants getConstantsConfig() {
        return adConfig;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    public boolean isConfigured() {
        return getConstantsConfig().isConfigured();
    }

    @Override
    public String providerType() {
        return getConstantsConfig().providerType();
    }

    @Override
    protected AbstractTokenUtil getTokenUtils() {
        return adTokenUtils;
    }

    @Override
    public Set<String> scopes() {
        return getConstantsConfig().scopes();
    }

    @Override
    public String getName() {
        return getConstantsConfig().getProviderName();
    }

    public List<Identity> savedIdentities() {
        List<String> ids = adTokenUtils.fromHashSeparatedString(ADConstants.AD_ALLOWED_IDENTITIES.get());
        List<Identity> identities = new ArrayList<>();
        if (ids.isEmpty() || !isConfigured()) {
            return identities;
        }
        for(String id: ids){
            String[] split = id.split(":", 2);
            Identity identity = getIdentity(split[1], split[0]);
            if (identity != null) {
                identities.add(identity);
            }
        }
        return identities;
    }

    private String getAllowedIdentitiesFilter() {
        StringBuilder filter = new StringBuilder();
        String memberOf = "(memberof=";
        String dn = "(distinguishedName=";
        int identitySize = 0 ;

        List<Identity> identities = savedIdentities();
        for (Identity identity: identities){
            if (getConstantsConfig().getGroupScope().equalsIgnoreCase(identity.getExternalIdType())){
                identitySize = identitySize + 1;
                filter.append(memberOf);
                filter.append(identity.getExternalId());
                filter.append(")");
            } else if (getConstantsConfig().getUserScope().equalsIgnoreCase(identity.getExternalIdType())){
                identitySize = identitySize + 1;
                filter.append(dn);
                filter.append(identity.getExternalId());
                filter.append(")");
            }
        }

        if(identitySize > 1) {
            //add OR
            StringBuilder outer = new StringBuilder("(|");
            outer.append(filter.toString());
            outer.append(")");
            return outer.toString();
        }

        return filter.toString();
    }

    public String validateIdentities(List<Map<String, String>> identitiesGiven) {
        List<Identity> identities = getIdentities(identitiesGiven);
        return adTokenUtils.toHashSeparatedString(identities);
    }

}
