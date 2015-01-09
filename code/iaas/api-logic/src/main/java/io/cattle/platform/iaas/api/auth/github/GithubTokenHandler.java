package io.cattle.platform.iaas.api.auth.github;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.token.TokenService;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;

import com.netflix.config.DynamicStringProperty;

public class GithubTokenHandler {

    private JsonMapper jsonMapper;
    private TokenService tokenService;
    private AuthDao authDao;

    private static final String GITHUB_REQUEST_TOKEN = "code";
    private static final String CLIENT_ID = "client_id";
    private static final String CLIENT_SECRET = "client_secret";
    private static final String GITHUB_ACCOUNT_KIND = "user";
    private static final String GITHUB_EXTERNAL_TYPE = "github";
    private static final Long DAY_IN_MILLISECONDS = (long) (60 * 60 * 24 * 1000);

    public static final DynamicStringProperty GITHUB_URL = ArchaiusUtil.getString("api.auth.github.url");

    private static final DynamicStringProperty GITHUB_CLIENT_ID = ArchaiusUtil.getString("api.auth.github.client.id");
    private static final DynamicStringProperty GITHUB_CLIENT_SECRET = ArchaiusUtil.getString("api.auth.github.client.secret");
    private static final DynamicStringProperty GITHUB_USER_INFO_URL = ArchaiusUtil.getString("api.auth.github.user.url");

    public Token getGithubAccessToken(ApiRequest request) throws IOException {

        List<RequestParam> requestData = new ArrayList<>();

        String code = null;
        Map<String, Object> requestBody = CollectionUtils.toMap(request.getRequestObject());
        code = (String) requestBody.get(GITHUB_REQUEST_TOKEN);

        requestData.add(new RequestParam(CLIENT_ID, GITHUB_CLIENT_ID.get()));
        requestData.add(new RequestParam(CLIENT_SECRET, GITHUB_CLIENT_SECRET.get()));
        requestData.add(new RequestParam(GITHUB_REQUEST_TOKEN, code));

        Map<String, Object> jsonData = null;

        HttpResponse response = Request.Post(GITHUB_URL.get()).addHeader("Accept", "application/json").bodyForm(requestData).execute().returnResponse();
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            throw new ClientVisibleException(statusCode);
        }
        jsonData = jsonMapper.readValue(response.getEntity().getContent());

        if (jsonData.get("error") != null) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, (String) jsonData.get("error_description"));
        }

        AccountInfo accountInfo = getAccountInfo((String) jsonData.get("access_token"));
        if (StringUtils.isNotEmpty(accountInfo.accountId)) {
            jsonData.put("account_id", accountInfo.accountId);
        }

        String accountName = accountInfo.accountName;
        authDao.createAccount(accountName, GITHUB_ACCOUNT_KIND, accountInfo.accountId, GITHUB_EXTERNAL_TYPE);
        return new Token(tokenService.generateEncryptedToken(jsonData, new Date(System.currentTimeMillis() + DAY_IN_MILLISECONDS)));
    }

    private AccountInfo getAccountInfo(String token) throws IOException {

        Map<String, Object> jsonData = null;

        HttpResponse response = Request.Get(GITHUB_USER_INFO_URL.get()).addHeader("Authorization", "token " + token).addHeader("Accept", "application/json")
                .execute().returnResponse();
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            throw new ClientVisibleException(statusCode);
        }
        jsonData = jsonMapper.readValue(response.getEntity().getContent());
        String accountId = ObjectUtils.toString(jsonData.get("id"));
        String accountName = ObjectUtils.toString(jsonData.get("username"));
        return new AccountInfo(accountId, accountName);
    }

    @Inject
    public void setTokenService(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
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
