package io.cattle.platform.iaas.api.auth.github;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.github.resource.GithubAccountInfo;
import io.cattle.platform.iaas.api.auth.github.resource.Token;
import io.cattle.platform.token.TokenService;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringProperty;

public class GithubTokenHandler {

    private TokenService tokenService;
    private AuthDao authDao;
    private GithubClient client;

    private static final String GITHUB_REQUEST_TOKEN = "code";
    private static final String GITHUB_USER_ACCOUNT_KIND = "user";
    private static final String GITHUB_ADMIN_ACCOUNT_KIND = "admin";
    private static final String GITHUB_EXTERNAL_TYPE = "github";
    private static final Long DAY_IN_MILLISECONDS = (long) (60 * 60 * 24 * 1000);

    private static final DynamicBooleanProperty SECURITY = ArchaiusUtil.getBoolean("api.security.enabled");
    private static final DynamicStringProperty WHITELISTED_ORGS = ArchaiusUtil.getString("api.auth.github.allowed.orgs");
    private static final DynamicStringProperty WHITELISTED_USERS = ArchaiusUtil.getString("api.auth.github.allowed.users");

    public Token getGithubAccessToken(ApiRequest request) throws IOException {
        Map<String, Object> requestBody = CollectionUtils.toMap(request.getRequestObject());
        String code = ObjectUtils.toString(requestBody.get(GITHUB_REQUEST_TOKEN));
        Map<String, Object> jsonData = client.getAccessToken(code);
        String token = (String) jsonData.get("access_token");
        List<String> idList = new ArrayList<>();
        List<String> orgNames = new ArrayList<>();

        GithubAccountInfo userAccountInfo = client.getUserAccountInfo(token);
        List<GithubAccountInfo> orgAccountInfo = client.getOrgAccountInfo(token);

        idList.add(userAccountInfo.getAccountId());

        for (GithubAccountInfo info : orgAccountInfo) {
            idList.add(info.getAccountId());
            orgNames.add(info.getAccountName());
        }

        if (SECURITY.get()) {
            if (null == getWhitelistedUser(idList)) {
                throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
            }
            Account userAccount = authDao.getAccountByExternalId(userAccountInfo.getAccountId(), GITHUB_EXTERNAL_TYPE);
            if (null == userAccount) {
                authDao.createAccount(userAccountInfo.getAccountName(), GITHUB_USER_ACCOUNT_KIND, userAccountInfo.getAccountId(), GITHUB_EXTERNAL_TYPE);
            }
        } else {
            Account admin = authDao.getAdminAccount();
            authDao.updateAccount(admin, null, GITHUB_ADMIN_ACCOUNT_KIND, userAccountInfo.getAccountId(), GITHUB_EXTERNAL_TYPE);
        }

        jsonData.put("account_id", userAccountInfo.getAccountId());
        jsonData.put("whitelist", idList);
        Date expiry = new Date(System.currentTimeMillis() + DAY_IN_MILLISECONDS);

        return new Token(tokenService.generateEncryptedToken(jsonData, expiry), userAccountInfo.getAccountName(), orgNames, null, null);
    }

    protected String getWhitelistedUser(List<String> idList) {
        if (idList == null) {
            return null;
        }
        List<String> whitelistedValues = fromCommaSeparatedString(WHITELISTED_ORGS.get());
        whitelistedValues.addAll(fromCommaSeparatedString(WHITELISTED_USERS.get()));
        Collection<String> whitelistedIds = Collections2.transform(whitelistedValues, new Function<String, String>() {
            @Override
            public String apply(String arg) {
                return arg.split("[:]")[1];
            }
        });
        List<String> whitelist = Lists.newArrayList(whitelistedIds);
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

    @Inject
    public void setGithubClient(GithubClient client) {
        this.client = client;
    }

    @Inject
    public void setTokenService(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Inject
    public void setAuthDao(AuthDao authDao) {
        this.authDao = authDao;
    }

    class AccountInfo {
        String accountId;
        String accountName;

        AccountInfo(String accountId, String accountName) {
            this.accountId = accountId;
            this.accountName = accountName;
        }
    }
}
