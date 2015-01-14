package io.cattle.platform.iaas.api.auth.github;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;

import com.netflix.config.DynamicStringProperty;

public class GithubClient {

    private JsonMapper jsonMapper;

    private static final String GITHUB_REQUEST_TOKEN = "code";
    private static final String CLIENT_ID = "client_id";
    private static final String CLIENT_SECRET = "client_secret";

    public static final DynamicStringProperty GITHUB_URL = ArchaiusUtil.getString("api.auth.github.url");

    private static final DynamicStringProperty GITHUB_CLIENT_ID = ArchaiusUtil.getString("api.auth.github.client.id");
    private static final DynamicStringProperty GITHUB_CLIENT_SECRET = ArchaiusUtil.getString("api.auth.github.client.secret");
    private static final DynamicStringProperty GITHUB_USER_INFO_URL = ArchaiusUtil.getString("api.auth.github.user.url");
    private static final DynamicStringProperty GITHUB_ORG_INFO_URL = ArchaiusUtil.getString("api.auth.github.org.url");
    private static final DynamicStringProperty GITHUB_UNAUTH_USER_URL = ArchaiusUtil.getString("api.github.user.url");
    private static final DynamicStringProperty GITHUB_UNAUTH_ORG_URL = ArchaiusUtil.getString("api.github.org.url");

    public Map<String, Object> getAccessToken(String code) {
        List<RequestParam> requestData = new ArrayList<>();

        if (StringUtils.isEmpty(code)) {
            throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, "code=Missing Required");
        }

        requestData.add(new RequestParam(CLIENT_ID, GITHUB_CLIENT_ID.get()));
        requestData.add(new RequestParam(CLIENT_SECRET, GITHUB_CLIENT_SECRET.get()));
        requestData.add(new RequestParam(GITHUB_REQUEST_TOKEN, code));

        Map<String, Object> jsonData = null;

        try {
            HttpResponse response = Request.Post(GITHUB_URL.get()).addHeader("Accept", "application/json").bodyForm(requestData).execute().returnResponse();
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                throw new ClientVisibleException(statusCode);
            }
            jsonData = jsonMapper.readValue(response.getEntity().getContent());
        } catch (Exception e) {
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        if (jsonData.get("error") != null) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, (String) jsonData.get("error_description"));
        }

        return jsonData;
    }

    public GithubAccountInfo getUserAccountInfo(String token) {
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        Map<String, Object> jsonData = null;

        try {
            HttpResponse response = Request.Get(GITHUB_USER_INFO_URL.get()).addHeader("Authorization", "token " + token)
                    .addHeader("Accept", "application/json").execute().returnResponse();
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                throw new ClientVisibleException(statusCode);
            }
            jsonData = jsonMapper.readValue(response.getEntity().getContent());
        } catch (Exception e) {
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        String accountId = ObjectUtils.toString(jsonData.get("id"));
        String accountName = ObjectUtils.toString(jsonData.get("login"));
        return new GithubAccountInfo(accountId, accountName);
    }

    public List<GithubAccountInfo> getOrgAccountInfo(String token) {
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        List<GithubAccountInfo> orgInfoList = new ArrayList<>();
        List<Map<String, Object>> jsonData = null;

        try {
            HttpResponse response = Request.Get(GITHUB_ORG_INFO_URL.get()).addHeader("Authorization", "token " + token).addHeader("Accept", "application/json")
                    .execute().returnResponse();
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                throw new ClientVisibleException(statusCode);
            }
            jsonData = jsonMapper.readCollectionValue(response.getEntity().getContent(), List.class, Map.class);
        } catch (Exception e) {
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        for (Map<String, Object> orgObject : jsonData) {
            String accountId = ObjectUtils.toString(orgObject.get("id"));
            String accountName = ObjectUtils.toString(orgObject.get("login"));
            orgInfoList.add(new GithubAccountInfo(accountId, accountName));
        }
        return orgInfoList;
    }

    public GithubAccountInfo getUserIdByName(String username) {
        if (StringUtils.isEmpty(username)) {
            return null;
        }
        Map<String, Object> jsonData = null;

        try {
            HttpResponse response = Request.Get(GITHUB_UNAUTH_USER_URL.get() + username).addHeader("Accept", "application/json").execute().returnResponse();
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                throw new ClientVisibleException(statusCode);
            }
            jsonData = CollectionUtils.toMap(jsonMapper.readValue(response.getEntity().getContent(), Map.class));
        } catch (Exception e) {
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        String accountId = ObjectUtils.toString(jsonData.get("id"));
        String accountName = ObjectUtils.toString(jsonData.get("login"));
        return new GithubAccountInfo(accountId, accountName);

    }

    public GithubAccountInfo getOrgIdByName(String org) {
        if (StringUtils.isEmpty(org)) {
            return null;
        }
        Map<String, Object> jsonData = null;

        try {
            HttpResponse response = Request.Get(GITHUB_UNAUTH_ORG_URL.get() + org).addHeader("Accept", "application/json").execute().returnResponse();
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                throw new ClientVisibleException(statusCode);
            }
            jsonData = CollectionUtils.toMap(jsonMapper.readValue(response.getEntity().getContent(), Map.class));
        } catch (Exception e) {
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        String accountId = ObjectUtils.toString(jsonData.get("id"));
        String accountName = ObjectUtils.toString(jsonData.get("login"));
        return new GithubAccountInfo(accountId, accountName);
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }
}
