package io.cattle.platform.iaas.api.auth.github;

import io.cattle.platform.api.auth.ExternalId;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.TokenHandler;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.github.constants.GithubConstants;
import io.cattle.platform.iaas.api.auth.github.resource.GithubAccountInfo;
import io.cattle.platform.iaas.api.auth.github.resource.TeamAccountInfo;
import io.cattle.platform.iaas.api.auth.github.resource.Token;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.token.TokenService;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicLongProperty;

public class GithubTokenHandler implements TokenHandler {

    @Inject
    private TokenService tokenService;
    @Inject
    private AuthDao authDao;
    @Inject
    private GithubClient githubClient;

    private static final DynamicLongProperty TOKEN_EXPIRY_MILLIS = ArchaiusUtil.getLong("api.auth.jwt.token.expiry");
    private static final DynamicBooleanProperty SECURITY = ArchaiusUtil.getBoolean("api.security.enabled");

    @Inject
    ProjectResourceManager projectResourceManager;
    @Inject
    ObjectManager objectManager;
    @Inject
    GithubUtils githubUtils;

    public Token getGithubToken(String accessToken) throws IOException {
        List<String> idList = new ArrayList<>();
        List<String> orgNames = new ArrayList<>();
        List<String> teamIds = new ArrayList<>();
        List<String> orgIds = new ArrayList<>();
        Map<String, String> teamToOrg = new HashMap<>();
        List<TeamAccountInfo> teamsAccountInfo = new ArrayList<>();
        GithubAccountInfo userAccountInfo = githubClient.getUserAccountInfo(accessToken);
        List<GithubAccountInfo> orgAccountInfo = githubClient.getOrgAccountInfo(accessToken);
        Set<ExternalId> externalIds = new HashSet<>();

        idList.add(userAccountInfo.getAccountId());
        externalIds.add(new ExternalId(userAccountInfo.getAccountId(), GithubConstants.USER_SCOPE, userAccountInfo.getAccountName()));

        for (GithubAccountInfo info : orgAccountInfo) {
            idList.add(info.getAccountId());
            orgNames.add(info.getAccountName());
            orgIds.add(info.getAccountId());
            teamsAccountInfo.addAll(githubClient.getOrgTeamInfo(accessToken, info.getAccountName()));
            externalIds.add(new ExternalId(info.getAccountId(), GithubConstants.ORG_SCOPE, info.getAccountName()));
        }

        for (TeamAccountInfo info : teamsAccountInfo) {
            teamIds.add(info.getId());
            teamToOrg.put(info.getId(), info.getOrg());
            idList.add(info.getId());
            externalIds.add(new ExternalId(info.getId(), GithubConstants.TEAM_SCOPE, info.getOrg() + ":" + info.getName()));
        }

        Account account;
        boolean hasAccessToAProject = authDao.hasAccessToAnyProject(externalIds, false, null);
        if (SECURITY.get()) {
            githubUtils.isAllowed(idList, externalIds);
            account = authDao.getAccountByExternalId(userAccountInfo.getAccountId(), GithubConstants.USER_SCOPE);
            if (null == account) {
                account = authDao.createAccount(userAccountInfo.getAccountName(), AccountConstants.USER_KIND, userAccountInfo.getAccountId(),
                        GithubConstants.USER_SCOPE);
                if (!hasAccessToAProject) {
                    projectResourceManager.createProjectForUser(account);
                }
            }
        } else {
            account = authDao.getAdminAccount();
            authDao.updateAccount(account, null, AccountConstants.ADMIN_KIND, userAccountInfo.getAccountId(), GithubConstants.USER_SCOPE);
            authDao.ensureAllProjectsHaveNonRancherIdMembers(new ExternalId(userAccountInfo.getAccountId(), GithubConstants.USER_SCOPE,
                    userAccountInfo.getAccountName()));
        }
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put(GithubConstants.ACCOUNT_ID, userAccountInfo.getAccountId());
        jsonData.put("teamToOrg", teamToOrg);
        jsonData.put("username", userAccountInfo.getAccountName());
        jsonData.put("team_ids", teamIds);
        jsonData.put("org_ids", orgIds);
        jsonData.put("idList", idList);
        DataAccessor.fields(account).withKey(GithubConstants.GITHUB_ACCESS_TOKEN).set(accessToken);
        objectManager.persist(account);
        account = objectManager.reload(account);
        String accountId = (String) ApiContext.getContext().getIdFormatter().formatId(objectManager.getType(Account.class), account.getId());
        Date expiry = new Date(System.currentTimeMillis() + TOKEN_EXPIRY_MILLIS.get());
        String jwt = tokenService.generateEncryptedToken(jsonData, expiry);
        return new Token(jwt, userAccountInfo.getAccountName(), orgNames, teamsAccountInfo, null, null,
                account.getKind(), accountId);
    }

    @Override
    public Token getToken(ApiRequest request) throws IOException {
        Map<String, Object> requestBody = CollectionUtils.toMap(request.getRequestObject());
        String code = ObjectUtils.toString(requestBody.get(GithubConstants.GITHUB_REQUEST_CODE));
        String accessToken = githubClient.getAccessToken(code);
        return getGithubToken(accessToken);
    }


}
