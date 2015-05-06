package io.cattle.platform.iaas.api.auth.github;

import io.cattle.platform.api.auth.ExternalId;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.TokenHandler;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.github.resource.GithubAccountInfo;
import io.cattle.platform.iaas.api.auth.github.resource.TeamAccountInfo;
import io.cattle.platform.iaas.api.auth.github.resource.Token;
import io.cattle.platform.object.ObjectManager;
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
    private GithubClient client;

    private static final String GITHUB_REQUEST_TOKEN = "code";

    private static final DynamicLongProperty TOKEN_EXPIRY_MILLIS = ArchaiusUtil.getLong("api.auth.jwt.token.expiry");
    private static final DynamicBooleanProperty SECURITY = ArchaiusUtil.getBoolean("api.security.enabled");

    @Inject
    ProjectResourceManager projectResourceManager;
    @Inject
    ObjectManager objectManager;
    @Inject
    GithubUtils githubUtils;

    public Token getGithubAccessToken(ApiRequest request) throws IOException {
        Map<String, Object> requestBody = CollectionUtils.toMap(request.getRequestObject());
        String code = ObjectUtils.toString(requestBody.get(GITHUB_REQUEST_TOKEN));
        Map<String, Object> jsonData = client.getAccessToken(code);
        String token = (String) jsonData.get("access_token");
        List<String> idList = new ArrayList<>();
        List<String> orgNames = new ArrayList<>();
        List<String> teamIds = new ArrayList<>();
        List<String> orgIds = new ArrayList<>();
        Map<String, String> teamToOrg = new HashMap<>();
        List<TeamAccountInfo> teamsAccountInfo = new ArrayList<>();
        GithubAccountInfo userAccountInfo = client.getUserAccountInfo(token);
        List<GithubAccountInfo> orgAccountInfo = client.getOrgAccountInfo(token);
        Set<ExternalId> externalIds = new HashSet<>();

        idList.add(userAccountInfo.getAccountId());
        externalIds.add(new ExternalId(userAccountInfo.getAccountId(), GithubUtils.USER_SCOPE, userAccountInfo.getAccountName()));

        for (GithubAccountInfo info : orgAccountInfo) {
            idList.add(info.getAccountId());
            orgNames.add(info.getAccountName());
            orgIds.add(info.getAccountId());
            teamsAccountInfo.addAll(client.getOrgTeamInfo(token, info.getAccountName()));
            externalIds.add(new ExternalId(info.getAccountId(), GithubUtils.ORG_SCOPE, info.getAccountName()));
        }

        for (TeamAccountInfo info : teamsAccountInfo) {
            teamIds.add(info.getId());
            teamToOrg.put(info.getId(), info.getOrg());
            idList.add(info.getId());
            externalIds.add(new ExternalId(info.getId(), GithubUtils.TEAM_SCOPE, info.getOrg() + ":" + info.getName()));
        }

        Account account;
        boolean hasAccessToAProject = authDao.hasAccessToAnyProject(externalIds, false, null);
        if (SECURITY.get() && githubUtils.isAllowed(idList, externalIds)) {
            account = authDao.getAccountByExternalId(userAccountInfo.getAccountId(), GithubUtils.USER_SCOPE);
            if (null == account) {
                account = authDao.createAccount(userAccountInfo.getAccountName(), AccountConstants.USER_KIND, userAccountInfo.getAccountId(),
                        GithubUtils.USER_SCOPE);
                if (!hasAccessToAProject) {
                    projectResourceManager.createProjectForUser(account);
                }
            }
        } else {
            account = authDao.getAdminAccount();
            authDao.updateAccount(account, null, AccountConstants.ADMIN_KIND, userAccountInfo.getAccountId(), GithubUtils.USER_SCOPE);
            authDao.ensureAllProjectsHaveNonRancherIdMembers(new ExternalId(userAccountInfo.getAccountId(), GithubUtils.USER_SCOPE,
                    userAccountInfo.getAccountName()));
        }
        account = objectManager.reload(account);
        jsonData.put("account_id", userAccountInfo.getAccountId());
        jsonData.put("teamToOrg", teamToOrg);
        jsonData.put("username", userAccountInfo.getAccountName());
        jsonData.put("team_ids", teamIds);
        jsonData.put("org_ids", orgIds);
        jsonData.put("idList", idList);
        String accountId = (String) ApiContext.getContext().getIdFormatter().formatId(objectManager.getType(Account.class), account.getId());
        Date expiry = new Date(System.currentTimeMillis() + TOKEN_EXPIRY_MILLIS.get());
        return new Token(tokenService.generateEncryptedToken(jsonData, expiry), userAccountInfo.getAccountName(), orgNames, teamsAccountInfo, null, null,
                account.getKind(), accountId);
    }

    @Override
    public Token getToken(ApiRequest request) throws IOException {
        return getGithubAccessToken(request);
    }
}
