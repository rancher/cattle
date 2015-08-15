package io.cattle.platform.iaas.api.auth.integration.github;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.TokenUtils;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.integration.github.resource.GithubAccountInfo;
import io.cattle.platform.iaas.api.auth.integration.github.resource.GithubClient;
import io.cattle.platform.iaas.api.auth.integration.github.resource.TeamAccountInfo;
import io.cattle.platform.iaas.api.auth.identity.Token;
import io.cattle.platform.iaas.api.auth.integration.interfaces.TokenCreator;
import io.cattle.platform.iaas.api.auth.projects.ProjectResourceManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.token.TokenService;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.netflix.config.DynamicLongProperty;

public class GithubTokenCreator extends GithubConfigurable implements TokenCreator {

    private static final DynamicLongProperty TOKEN_EXPIRY_MILLIS = ArchaiusUtil.getLong("api.auth.jwt.token.expiry");
    @Inject
    ProjectResourceManager projectResourceManager;
    @Inject
    ObjectManager objectManager;
    @Inject
    GithubUtils githubUtils;
    @Inject
    GithubClient githubClient;
    @Inject
    private TokenService tokenService;
    @Inject
    private AuthDao authDao;

    public Token getGithubToken(String accessToken) {
        if (!isConfigured()) {
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, GithubConstants.CONFIG, "No Github Client id and secret found.", null);
        }
        List<TeamAccountInfo> teamsAccountInfo = new ArrayList<>();
        GithubAccountInfo userAccountInfo = githubClient.getUserAccountInfo(accessToken);
        List<GithubAccountInfo> orgAccountInfo = githubClient.getOrgAccountInfo(accessToken);
        Set<Identity> identities = new HashSet<>();

        Identity user = userAccountInfo.toIdentity(GithubConstants.USER_SCOPE);
        identities.add(user);

        for (GithubAccountInfo info : orgAccountInfo) {
            teamsAccountInfo.addAll(githubClient.getOrgTeamInfo(accessToken, info.getAccountName()));
            Identity org = info.toIdentity(GithubConstants.ORG_SCOPE);
            identities.add(org);
        }

        for (TeamAccountInfo info : teamsAccountInfo) {
            Identity team = new Identity(GithubConstants.TEAM_SCOPE, info.getId(), info.getOrg() + ":" + info.getName(),
                    null, null, null);
            identities.add(team);
        }
        List<String> idList = githubUtils.identitiesToIdList(identities);
        Account account = null;
        boolean hasAccessToAProject = authDao.hasAccessToAnyProject(identities, false, null);
        if (SecurityConstants.SECURITY.get()) {
            if (githubUtils.isAllowed(idList, identities)) {
                account = authDao.getAccountByExternalId(userAccountInfo.getAccountId(), GithubConstants.USER_SCOPE);
                if (null == account) {
                    account = authDao.createAccount(userAccountInfo.getAccountName(), AccountConstants.USER_KIND, userAccountInfo.getAccountId(),
                            GithubConstants.USER_SCOPE);
                    if (!hasAccessToAProject) {
                        projectResourceManager.createProjectForUser(account);
                    }
                }
            }
        } else {
            account = authDao.getAdminAccount();
            authDao.updateAccount(account, null, AccountConstants.ADMIN_KIND, userAccountInfo.getAccountId(), GithubConstants.USER_SCOPE);
            authDao.ensureAllProjectsHaveNonRancherIdMembers(userAccountInfo.toIdentity(GithubConstants.USER_SCOPE));
        }
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put(TokenUtils.TOKEN, GithubConstants.GITHUB_JWT);
        jsonData.put(TokenUtils.ACCOUNT_ID, userAccountInfo.getAccountId());
        jsonData.put(GithubConstants.USERNAME, userAccountInfo.getAccountName());
        jsonData.put(TokenUtils.ID_LIST, idList);
        DataAccessor.fields(account).withKey(GithubConstants.GITHUB_ACCESS_TOKEN).set(accessToken);
        objectManager.persist(account);
        account = objectManager.reload(account);
        String accountId = (String) ApiContext.getContext().getIdFormatter().formatId(objectManager.getType(Account.class), account.getId());
        Date expiry = new Date(System.currentTimeMillis() + TOKEN_EXPIRY_MILLIS.get());
        String jwt = tokenService.generateEncryptedToken(jsonData, expiry);
        //LEGACY: Used for old Implementation of projects/ Identities. Remove when vincent changes to new api.
        return new Token(jwt, user.getName(), null, teamsAccountInfo, SecurityConstants.SECURITY.get(),
                GithubConstants.GITHUB_CLIENT_ID.get(), account.getKind(), SecurityConstants.AUTH_PROVIDER.get(),
                accountId, new ArrayList<>(identities), user);
    }

    @Override
    public Token createToken(ApiRequest request) {
        Map<String, Object> requestBody = CollectionUtils.toMap(request.getRequestObject());
        String code = ObjectUtils.toString(requestBody.get(SecurityConstants.CODE));
        String accessToken = githubClient.getAccessToken(code);
        if (StringUtils.isBlank(accessToken)){
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, getName(), "Failed to get accessToken.",
                    null);
        }
        return getGithubToken(accessToken);
    }

    @Override
    public String providerType() {
        return GithubConstants.CONFIG;
    }

    @Override
    public String getName() {
        return GithubConstants.TOKEN_CREATOR;
    }
}
