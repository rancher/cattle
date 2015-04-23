package io.cattle.platform.iaas.api.auth.github;

import io.cattle.platform.api.auth.ExternalId;
import io.cattle.platform.archaius.util.ArchaiusUtil;
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
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicStringProperty;

public class GithubTokenHandler implements TokenHandler {

    @Inject
    private TokenService tokenService;
    @Inject
    private AuthDao authDao;
    @Inject
    private GithubClient client;

    private static final String GITHUB_REQUEST_TOKEN = "code";
    private static final String GITHUB_USER_ACCOUNT_KIND = "user";
    private static final String GITHUB_ADMIN_ACCOUNT_KIND = "admin";
    private static final String GITHUB_EXTERNAL_TYPE = "github";

    private static final DynamicLongProperty TOKEN_EXPIRY_MILLIS = ArchaiusUtil.getLong("api.auth.jwt.token.expiry");
    private static final DynamicBooleanProperty SECURITY = ArchaiusUtil.getBoolean("api.security.enabled");
    private static final DynamicStringProperty WHITELISTED_ORGS = ArchaiusUtil.getString("api.auth.github.allowed.orgs");
    private static final DynamicStringProperty WHITELISTED_USERS = ArchaiusUtil.getString("api.auth.github.allowed.users");
    private static final DynamicStringProperty ACCESS_MODE = ArchaiusUtil.getString("api.auth.github.access.mode");

    @Inject
    ProjectResourceManager projectResourceManager;
    @Inject
    ObjectManager objectManager;

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
            externalIds.add(new ExternalId(info.getId(), GithubUtils.ORG_SCOPE, info.getOrg() + ":" + info.getName()));
        }

        Account account = null;
        boolean whiteListed = getWhitelistedUser(idList) != null;
        boolean hasAccessToAProject = authDao.hasAccessToAnyProject(externalIds, false, null);
        if (SECURITY.get()) {
            switch (ACCESS_MODE.get()) {
                case "restricted":
                    if (whiteListed) {
                        break;
                    } else if (!whiteListed && !hasAccessToAProject) {
                        throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
                    }
                    break;
                case "unrestricted":
                whiteListed = true;
                    break;
                default:
                    throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
            }
            account = authDao.getAccountByExternalId(userAccountInfo.getAccountId(), GITHUB_EXTERNAL_TYPE);
            if (null == account) {
                account = authDao.createAccount(userAccountInfo.getAccountName(), GITHUB_USER_ACCOUNT_KIND, userAccountInfo.getAccountId(),
                        GITHUB_EXTERNAL_TYPE);
                projectResourceManager.createDefaultProject(account);
            }
            if (whiteListed && !hasAccessToAProject) {
                    projectResourceManager.createDefaultProject(account);
            }
        } else {
            account = authDao.getAdminAccount();
            authDao.updateAccount(account, null, GITHUB_ADMIN_ACCOUNT_KIND, userAccountInfo.getAccountId(), GITHUB_EXTERNAL_TYPE);
        }
        account = objectManager.reload(account);
        jsonData.put("account_id", userAccountInfo.getAccountId());
        jsonData.put("external_ids", externalIds);
        jsonData.put("teamToOrg", teamToOrg);
        jsonData.put("username", userAccountInfo.getAccountName());
        jsonData.put("team_ids", teamIds);
        jsonData.put("org_ids", orgIds);
        String defaultProjectId = (String) ApiContext.getContext().getIdFormatter().formatId(objectManager.getType(Account.class), account.getProjectId());
        String accountId = (String) ApiContext.getContext().getIdFormatter().formatId(objectManager.getType(Account.class), account.getId());
        Date expiry = new Date(System.currentTimeMillis() + TOKEN_EXPIRY_MILLIS.get());
        return new Token(tokenService.generateEncryptedToken(jsonData, expiry), userAccountInfo.getAccountName(), orgNames, teamsAccountInfo, null, null,
                account.getKind(), defaultProjectId, accountId);
    }

    protected String getWhitelistedUser(List<String> idList) {
        if (idList == null) {
            return null;
        }
        if (StringUtils.equals(WHITELISTED_USERS.get(), "*")) {
            return "*";
        }
        List<String> whitelistedValues = fromCommaSeparatedString(WHITELISTED_ORGS.get());
        whitelistedValues.addAll(fromCommaSeparatedString(WHITELISTED_USERS.get()));
        Collection<String> whitelistedIds = Collections2.transform(whitelistedValues, new Function<String, String>() {
            @Override
            public String apply(String arg) {
                return arg.split("[:]")[1];
            }
        });
        Set<String> whitelist = new HashSet<>(whitelistedIds);
        for (String id : idList) {
            if (whitelist.contains(id)) {
                return id;
            }
        }
        return null;
    }

    protected List<String> fromCommaSeparatedString(String string) {
        if (StringUtils.isEmpty(string)) {
            return new ArrayList<>();
        }
        List<String> strings = new ArrayList<String>();
        String[] splitted = string.split(",");
        for (int i = 0; i < splitted.length; i++) {
            String element = splitted[i].trim();
            strings.add(element);
        }
        return strings;
    }

    @Override
    public Token getToken(ApiRequest request) throws IOException {
        return getGithubAccessToken(request);
    }
}
