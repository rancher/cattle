package io.cattle.platform.iaas.api.auth.github;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.iaas.api.auth.github.resource.GithubAccountInfo;
import io.cattle.platform.iaas.api.auth.github.resource.TeamAccountInfo;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.message.BasicNameValuePair;

import com.netflix.config.DynamicStringProperty;

public class GithubClient {

    private static final String GITHUB_REQUEST_TOKEN = "code";
    private static final String CLIENT_ID = "client_id";
    private static final String CLIENT_SECRET = "client_secret";

    private static final DynamicStringProperty GITHUB_CLIENT_ID = ArchaiusUtil.getString("api.auth.github.client.id");
    private static final DynamicStringProperty GITHUB_CLIENT_SECRET = ArchaiusUtil.getString("api.auth.github.client.secret");

    @Inject
    private JsonMapper jsonMapper;

    @Inject
    private GithubUtils githubUtils;

    private static final Log logger = LogFactory.getLog(GithubClient.class);

    public Map<String, Object> getAccessToken(String code) throws IOException {
        List<BasicNameValuePair> requestData = new ArrayList<>();

        requestData.add(new BasicNameValuePair(CLIENT_ID, GITHUB_CLIENT_ID.get()));
        requestData.add(new BasicNameValuePair(CLIENT_SECRET, GITHUB_CLIENT_SECRET.get()));
        requestData.add(new BasicNameValuePair(GITHUB_REQUEST_TOKEN, code));

        Map<String, Object> jsonData = null;

        HttpResponse response = Request.Post(githubUtils.getURL(GithubClientEndpoints.TOKEN)).addHeader("Accept", "application/json").bodyForm(requestData)
                .execute().returnResponse();
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            noGithub(statusCode);
        }
        jsonData = jsonMapper.readValue(response.getEntity().getContent());

        if (jsonData.get("error") != null) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, (String) jsonData.get("error_description"));
        }

        return jsonData;
    }

    public GithubAccountInfo getUserAccountInfo(String token) throws IOException {
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        Map<String, Object> jsonData = null;

        HttpResponse response = Request.Get(githubUtils.getURL(GithubClientEndpoints.USER_INFO)).addHeader("Authorization", "token " + token).addHeader(
                "Accept", "application/json").execute().returnResponse();
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            noGithub(statusCode);
        }
        jsonData = jsonMapper.readValue(response.getEntity().getContent());

        String accountId = ObjectUtils.toString(jsonData.get("id"));
        String accountName = ObjectUtils.toString(jsonData.get("login"));
        return new GithubAccountInfo(accountId, accountName);
    }

    public List<GithubAccountInfo> getOrgAccountInfo(String token) throws IOException {
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        List<GithubAccountInfo> orgInfoList = new ArrayList<>();
        List<Map<String, Object>> jsonData = null;

        HttpResponse response = Request.Get(githubUtils.getURL(GithubClientEndpoints.ORG_INFO)).addHeader("Authorization", "token " + token).addHeader(
                "Accept", "application/json").execute().returnResponse();
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            noGithub(statusCode);
        }
        jsonData = jsonMapper.readCollectionValue(response.getEntity().getContent(), List.class, Map.class);

        for (Map<String, Object> orgObject : jsonData) {
            String accountId = ObjectUtils.toString(orgObject.get("id"));
            String accountName = ObjectUtils.toString(orgObject.get("login"));
            orgInfoList.add(new GithubAccountInfo(accountId, accountName));
        }
        return orgInfoList;
    }

    public List<TeamAccountInfo> getOrgTeamInfo(String token, String org) throws IOException {
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        List<TeamAccountInfo> teamInfoList = new ArrayList<>();
        List<Map<String, Object>> jsonData = null;

        HttpResponse response = Request.Get(githubUtils.getURL(GithubClientEndpoints.ORGS) + org + "/teams").addHeader("Authorization", "token " + token)
                .addHeader("Accept", "application/json").execute().returnResponse();
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            noGithub(statusCode);
        }
        jsonData = jsonMapper.readCollectionValue(response.getEntity().getContent(), List.class, Map.class);

        for (Map<String, Object> orgObject : jsonData) {
            String accountId = ObjectUtils.toString(orgObject.get("id"));
            String accountName = ObjectUtils.toString(orgObject.get("name"));
            String slug = ObjectUtils.toString(orgObject.get("slug"));
            if (!StringUtils.equalsIgnoreCase("Owners", accountName)) {
                teamInfoList.add(new TeamAccountInfo(org, accountName, accountId, slug));
            }
        }
        return teamInfoList;
    }

    public GithubAccountInfo getUserIdByName(String username, String token) {
        try {
            if (StringUtils.isEmpty(username)) {
                return null;
            }
            HttpResponse response = Request.Get(githubUtils.getURL(GithubClientEndpoints.USERS) + username).addHeader("Accept", "application/json").addHeader(
                    "Authorization", "token " + token).execute().returnResponse();
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "GitHubError", "Non-200 Response from Github", "Status code from Github: "
                        + Integer.toString(statusCode));
            }
            Map<String, Object> jsonData = CollectionUtils.toMap(jsonMapper.readValue(response.getEntity().getContent(), Map.class));

            String accountId = ObjectUtils.toString(jsonData.get("id"));
            String accountName = ObjectUtils.toString(jsonData.get("login"));
            return new GithubAccountInfo(accountId, accountName);
        } catch (IOException e){
            logger.error(e);
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "GithubUnavailable", "Could not retrieve UserId from Github", null);
        }

    }

    public GithubAccountInfo getUserIdByName(String username) {
        String token = githubUtils.validateAndFetchGithubToken(githubUtils.getToken());
        return getUserIdByName(username, token);

    }

    public GithubAccountInfo getOrgIdByName(String org, String token) {
        try{
            if (StringUtils.isEmpty(org)) {
                return null;
            }
            HttpResponse response = Request.Get(githubUtils.getURL(GithubClientEndpoints.ORGS) + org).addHeader("Accept", "application/json").addHeader(
                    "Authorization", "token " + token).execute().returnResponse();
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "GitHubError", "Non-200 Response from Github", "Status code from Github: "
                        + Integer.toString(statusCode));
            }
            Map<String, Object> jsonData = CollectionUtils.toMap(jsonMapper.readValue(response.getEntity().getContent(), Map.class));

            String accountId = ObjectUtils.toString(jsonData.get("id"));
            String accountName = ObjectUtils.toString(jsonData.get("login"));
            return new GithubAccountInfo(accountId, accountName);
        } catch (IOException e){
            logger.error(e);
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "GithubUnavailable", "Could not retrieve orgId from Github", null);
        }
    }

    public GithubAccountInfo getOrgIdByName(String org) {
        String token = githubUtils.validateAndFetchGithubToken(githubUtils.getToken());
        return  getOrgIdByName(org, token);
    }

    public String getTeamOrgById(String id) {
        return  githubUtils.getTeamOrgById(id);
    }

    private void noGithub(Integer statusCode) {
        throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "GitHubError", "Non-200 Response from Github", "Status code from Github: "
                + Integer.toString(statusCode));
    }

}
