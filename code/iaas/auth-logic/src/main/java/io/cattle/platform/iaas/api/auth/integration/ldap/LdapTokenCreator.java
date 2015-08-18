package io.cattle.platform.iaas.api.auth.integration.ldap;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.TokenUtils;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.identity.Token;
import io.cattle.platform.iaas.api.auth.integration.interfaces.TokenCreator;
import io.cattle.platform.iaas.api.auth.projects.ProjectResourceManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.token.TokenService;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;

public class LdapTokenCreator extends LdapConfigurable implements TokenCreator {

    @Inject
    LdapIdentitySearchProvider ldapIdentitySearchProvider;
    @Inject
    AuthDao authDao;
    @Inject
    TokenService tokenService;
    @Inject
    ProjectResourceManager projectResourceManager;
    @Inject
    ObjectManager objectManager;

    @Inject
    LdapUtils ldapUtils;

    private Token getLdapToken(String username, String password) {
        if (!isConfigured()) {
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, LdapConstants.CONFIG, "Ldap Not Configured.", null);
        }
        Set<Identity> identities = ldapIdentitySearchProvider.getIdentities(username, password);
        return getTokenByIdentities(identities);
    }

    @Override
    public Token getToken(ApiRequest request) {
        Map<String, Object> requestBody = CollectionUtils.toMap(request.getRequestObject());
        if (!isConfigured()) {
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "LdapConfig", "LdapConfig is not Configured.", null);
        }
        String code = ObjectUtils.toString(requestBody.get(SecurityConstants.CODE));
        String[] split = code.split(":");
        if (split.length != 2) {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
        }
        return getLdapToken(split[0], split[1]);
    }

    @Override
    public String providerType() {
        return LdapConstants.CONFIG;
    }

    public Token getTokenByIdentities(Set<Identity> identities){
        if (!isConfigured()) {
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, LdapConstants.CONFIG, "Ldap Not Configured.", null);
        }
        Account account;
        Identity gotIdentity = null;
        for (Identity identity: identities){
            if (identity.getExternalIdType().equalsIgnoreCase(LdapConstants.USER_SCOPE)){
                gotIdentity = identity;
                break;
            }
        }
        if (gotIdentity == null) {
            throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
        }
        boolean hasAccessToAProject = authDao.hasAccessToAnyProject(identities, false, null);
        if (SecurityConstants.SECURITY.get()) {
            ldapUtils.isAllowed(ldapUtils.identitiesToIdList(identities), identities);
            account = authDao.getAccountByExternalId(gotIdentity.getExternalId(), LdapConstants.USER_SCOPE);
            if (null == account) {
                account = authDao.createAccount(gotIdentity.getLogin(), AccountConstants.USER_KIND, gotIdentity.getExternalId(),
                        LdapConstants.USER_SCOPE);
                if (!hasAccessToAProject) {
                    projectResourceManager.createProjectForUser(account);
                }
            }
        } else {
            account = authDao.getAdminAccount();
            authDao.updateAccount(account, null, AccountConstants.ADMIN_KIND, gotIdentity.getExternalId(), LdapConstants.USER_SCOPE);
            authDao.ensureAllProjectsHaveNonRancherIdMembers(gotIdentity);
        }
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put(TokenUtils.TOKEN, LdapConstants.LDAP_JWT);
        jsonData.put(TokenUtils.ACCOUNT_ID, gotIdentity.getExternalId());
        jsonData.put(TokenUtils.ID_LIST, ldapUtils.identitiesToIdList(identities));
        account = objectManager.reload(account);
        String accountId = (String) ApiContext.getContext().getIdFormatter().formatId(objectManager.getType(Account.class), account.getId());
        Date expiry = new Date(System.currentTimeMillis() + SecurityConstants.TOKEN_EXPIRY_MILLIS.get());
        String jwt = tokenService.generateEncryptedToken(jsonData, expiry);
        return new Token(jwt, SecurityConstants.AUTH_PROVIDER.get(), accountId, gotIdentity,
                new ArrayList<>(identities), SecurityConstants.SECURITY.get(), account.getKind());
    }
    @Override
    public String getName() {
        return LdapConstants.TOKEN_CREATOR;
    }
}
