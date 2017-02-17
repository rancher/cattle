package io.cattle.platform.iaas.api.auth.integration.ldap.OpenLDAP;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.AuthToken;
import io.cattle.platform.iaas.api.auth.AbstractTokenUtil;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.dao.AuthTokenDao;
import io.cattle.platform.iaas.api.auth.identity.Token;
import io.cattle.platform.iaas.api.auth.integration.interfaces.IdentityProvider;
import io.cattle.platform.iaas.api.auth.integration.ldap.LDAPIdentityProvider;
import io.cattle.platform.iaas.api.auth.integration.ldap.UserLoginFailureException;
import io.cattle.platform.iaas.api.auth.integration.ldap.interfaces.LDAPConstants;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenLDAPIdentityProvider extends LDAPIdentityProvider implements IdentityProvider {

    private static final Logger logger = LoggerFactory.getLogger(OpenLDAPIdentityProvider.class);
    @Inject
    OpenLDAPUtils openLDAPUtils;
    @Inject
    OpenLDAPTokenCreator ADTokenCreator;
    @Inject
    AuthTokenDao authTokenDao;
    private GenericObjectPool<LdapContext> contextPool;
    ExecutorService executorService;
    @Inject
    OpenLDAPConstantsConfig openLDAPConfig;


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
                LdapName dn;
                try {
                    dn = new LdapName(account.getExternalId());
                } catch (NamingException e) {
                    throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
                }
                Set<Identity> identities = getIdentities(dn);
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

    private Set<Identity> getIdentities(LdapName dn) {
        Set<Identity> identities = new HashSet<>();
        Attributes userAttributes, opAttributes = null;
        LdapContext context = getServiceContext();
        try {
            userAttributes = context.getAttributes(dn);
            if (!hasPermission(userAttributes)){
                throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
            }
            logger.trace("getIdentities: user attributes: " + userAttributes);
            try {
                String[] operationalAttrList = {"1.1", "+", "*"};
                opAttributes = context.getAttributes(dn, operationalAttrList);
                logger.trace("getIdentities: operational attributes: " + opAttributes);
            } catch (NamingException e) {
                logger.error("Exception trying to get operational attributes", e);
            }
        } catch (NamingException e) {
            throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
        } finally {
            if (context != null) {
                getContextPool().returnObject(context);
            }
        }
        Attribute memberOf = userAttributes.get(getConstantsConfig().getUserMemberAttribute());
        if(memberOf == null) {
            memberOf = opAttributes.get(getConstantsConfig().getUserMemberAttribute());
        }
        logger.trace("getIdentities: memberOf attribute: " + memberOf);
        try {
            if (!isType(userAttributes, getConstantsConfig().getUserObjectClass()))
            {
                return identities;
            }
            Identity user = attributesToIdentity(dn);
            if (user != null) {
                identities.add(user);
            }
            if (memberOf != null) {// null if this user belongs to no group at all
                for (int i = 0; i < memberOf.size(); i++) {
                    String query = "(&(" + getConstantsConfig().getGroupDNField() +
                            '=' + memberOf.get(i).toString() + ")(" + getConstantsConfig().objectClass() + '=' +
                            getConstantsConfig().getGroupObjectClass() + "))";
                    logger.trace("getIdentities: memberOf attribute query: "+query);
                    identities.addAll(resultsToIdentities(searchLdap(query)));
                }
            }

            Attribute groupMemberUserAttribute = userAttributes.get(getConstantsConfig().getGroupMemberUserAttribute());
            if(groupMemberUserAttribute == null) {
                groupMemberUserAttribute = opAttributes.get(getConstantsConfig().getGroupMemberUserAttribute());
            }
            logger.trace("getIdentities: groupMemberUserAttribute attribute: "+groupMemberUserAttribute);
            if (groupMemberUserAttribute != null && StringUtils.isNotBlank(groupMemberUserAttribute.toString())){
                String query = "(&(" + getConstantsConfig().getGroupMemberMappingAttribute()
                        + '=' + ((String)groupMemberUserAttribute.get())
                        + ")(" + getConstantsConfig().objectClass() + '='
                        + getConstantsConfig().getGroupObjectClass() + "))";
                logger.trace("getIdentities: groupMemberUserAttribute attribute query: "+query);
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
        String query = "(&(" + getConstantsConfig().getUserLoginField() + '=' + username + ")(" + getConstantsConfig().objectClass() + '='
                + getConstantsConfig().getUserObjectClass() + "))";
        List<Identity> users = resultsToIdentities(searchLdap(query));
        if (users.size() != 1){
            logger.error("Found no or multiple users for: " + username);
            throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
        } else {
            try {
                LdapContext userContext = login(users.get(0).getExternalId(), password);
                try {
                    if (userContext != null){
                        userContext.close();
                    }
                } catch (NamingException e) {
                    logger.error("Failed to close userContext. Reason: " + e.getExplanation());
                }
                try {
                    user = new LdapName(users.get(0).getExternalId());
                } catch (InvalidNameException e) {
                    logger.error("Distinguished name not found for user:" + username, e);
                    throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
                }
            } catch (UserLoginFailureException e) {
                logger.info("Failed to login to ldap user:" + username + " " + e.getUsername() + "Original cause: " + e.getMessage());
                throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
            }
        }
        return getIdentities(user);
    }

    @Override
    protected void setContextPool(GenericObjectPool<LdapContext> ldapContextGenericObjectPool) {
        this.contextPool = ldapContextGenericObjectPool;
    }

    @Override
    protected AbstractTokenUtil getTokenUtils() {
        return openLDAPUtils;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public ExecutorService getExecutorService() {
        return executorService;
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
    protected void notConfigured() {
        throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE,
                "NotConfigured", "Ldap is not configured", null);
    }

    @Override
    protected GenericObjectPool<LdapContext> getContextPool() {
        return this.contextPool;
    }

    @Override
    protected LDAPConstants getConstantsConfig() {
        return openLDAPConfig;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    public Set<String> scopes() {
        return getConstantsConfig().scopes();
    }

    @Override
    public String getName() {
        return getConstantsConfig().getProviderName();
    }

    public String validateIdentities(List<Map<String, String>> identitiesGiven) {
        List<Identity> identities = getIdentities(identitiesGiven);
        return openLDAPUtils.toHashSeparatedString(identities);
    }

    public List<Identity> savedIdentities() {
        List<String> ids = openLDAPUtils.fromHashSeparatedString(OpenLDAPConstants.LDAP_ALLOWED_IDENTITIES.get());
        List<Identity> identities = new ArrayList<>();
        if (ids.isEmpty() || !isConfigured()) {
            return identities;
        }
        for(String id: ids){
            String[] split = id.split(":", 2);
            identities.add(getIdentity(split[1], split[0]));
        }
        return identities;
    }
}
