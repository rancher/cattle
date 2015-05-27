package io.cattle.platform.iaas.api.auth.github;

import io.cattle.platform.iaas.api.auth.github.constants.GithubConstants;
import io.cattle.platform.iaas.api.auth.github.resource.GithubAccountInfo;
import io.cattle.platform.iaas.api.auth.github.resource.TeamAccountInfo;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
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

public class GithubClient {

    @Inject
    private JsonMapper jsonMapper;

    @Inject
    private GithubUtils githubUtils;

    private static final Log logger = LogFactory.getLog(GithubClient.class);

    private static final String LOGIN = "login";

    public String getAccessToken(String code) throws IOException {
        List<BasicNameValuePair> requestData = new ArrayList<>();

        if (StringUtils.isBlank(GithubConstants.GITHUB_CLIENT_ID.get()) || StringUtils.isBlank(GithubConstants.GITHUB_CLIENT_SECRET.get())){
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, GithubConstants.GITHUBCONFIG, "No Github Client id and secret found.", null);
        }

        requestData.add(new BasicNameValuePair(GithubConstants.CLIENT_ID, GithubConstants.GITHUB_CLIENT_ID.get()));
        requestData.add(new BasicNameValuePair(GithubConstants.CLIENT_SECRET, GithubConstants.GITHUB_CLIENT_SECRET.get()));
        requestData.add(new BasicNameValuePair(GithubConstants.GITHUB_REQUEST_CODE, code));

        Map<String, Object> jsonData;

        HttpResponse response = Request.Post(githubUtils.getURL(GithubClientEndpoints.TOKEN))
                .addHeader(GithubConstants.ACCEPT, GithubConstants.APPLICATION_JSON).bodyForm(requestData)
                .execute().returnResponse();
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            noGithub(statusCode);
        }
        jsonData = jsonMapper.readValue(response.getEntity().getContent());

        if (jsonData.get("error") != null) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, (String) jsonData.get("error_description"));
        }

        return (String) jsonData.get(GithubConstants.GITHUB_ACCESS_TOKEN);
    }

    public GithubAccountInfo getUserAccountInfo(String githubAccessToken) throws IOException {
        if (StringUtils.isEmpty(githubAccessToken)) {
            return null;
        }
        Map<String, Object> jsonData;

        HttpResponse response = Request.Get(githubUtils.getURL(GithubClientEndpoints.USER_INFO))
                .addHeader(GithubConstants.AUTHORIZATION, "token " + githubAccessToken)
                .addHeader(GithubConstants.ACCEPT, GithubConstants.APPLICATION_JSON).execute().returnResponse();
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            noGithub(statusCode);
        }
        jsonData = jsonMapper.readValue(response.getEntity().getContent());

        String accountId = ObjectUtils.toString(jsonData.get("id"));
        String accountName = ObjectUtils.toString(jsonData.get(LOGIN));
        return new GithubAccountInfo(accountId, accountName);
    }

    public List<GithubAccountInfo> getOrgAccountInfo(String githubAccessToken) throws IOException {
        if (StringUtils.isEmpty(githubAccessToken)) {
            return null;
        }
        List<GithubAccountInfo> orgInfoList = new ArrayList<>();
        List<Map<String, Object>> jsonData;

        HttpResponse response = Request.Get(githubUtils.getURL(GithubClientEndpoints.ORG_INFO))
                .addHeader(GithubConstants.AUTHORIZATION, "token " + githubAccessToken)
                .addHeader(GithubConstants.ACCEPT, GithubConstants.APPLICATION_JSON).execute().returnResponse();

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            noGithub(statusCode);
        }
        jsonData = jsonMapper.readCollectionValue(response.getEntity().getContent(), List.class, Map.class);

        for (Map<String, Object> orgObject : jsonData) {
            String accountId = ObjectUtils.toString(orgObject.get("id"));
            String accountName = ObjectUtils.toString(orgObject.get(LOGIN));
            orgInfoList.add(new GithubAccountInfo(accountId, accountName));
        }
        return orgInfoList;
    }

    public List<TeamAccountInfo> getOrgTeamInfo(String githubAccessToken, String org) throws IOException {
        if (StringUtils.isEmpty(githubAccessToken)) {
            return null;
        }
        List<TeamAccountInfo> teamInfoList = new ArrayList<>();
        List<Map<String, Object>> jsonData;

        HttpResponse response = Request.Get(githubUtils.getURL(GithubClientEndpoints.ORGS) + org + "/teams").addHeader(GithubConstants.AUTHORIZATION, "token " +
                "" + githubAccessToken).addHeader(GithubConstants.ACCEPT, GithubConstants.APPLICATION_JSON).execute().returnResponse();
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

    public GithubAccountInfo getUserIdByName(String username) {
        String githubAccessToken = (String) ApiContext.getContext().getApiRequest().getAttribute(GithubConstants.GITHUB_ACCESS_TOKEN);
        try {
            if (StringUtils.isEmpty(username)) {
                return null;
            }
            HttpResponse response = Request.Get(githubUtils.getURL(GithubClientEndpoints.USERS) + username)
                    .addHeader(GithubConstants.ACCEPT, GithubConstants.APPLICATION_JSON).addHeader(
                    GithubConstants.AUTHORIZATION, "token " + githubAccessToken).execute().returnResponse();
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, GithubConstants.GITHUB_ERROR,
                        "Non-200 Response from Github", "Status code from Github: " + Integer.toString(statusCode));
            }
            Map<String, Object> jsonData = CollectionUtils.toMap(jsonMapper.readValue(response.getEntity().getContent(), Map.class));

            String accountId = ObjectUtils.toString(jsonData.get("id"));
            String accountName = ObjectUtils.toString(jsonData.get(LOGIN));
            return new GithubAccountInfo(accountId, accountName);
        } catch (IOException e){
            logger.error(e);
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "GithubUnavailable", "Could not retrieve UserId from Github", null);
        }

    }

    public GithubAccountInfo getOrgIdByName(String org) {
        String gitHubAccessToken = (String) ApiContext.getContext().getApiRequest().getAttribute(GithubConstants.GITHUB_ACCESS_TOKEN);
        try{
            if (StringUtils.isEmpty(org)) {
                return null;
            }
            HttpResponse response = Request.Get(githubUtils.getURL(GithubClientEndpoints.ORGS) + org)
                    .addHeader(GithubConstants.ACCEPT, GithubConstants.APPLICATION_JSON)
                    .addHeader(GithubConstants.AUTHORIZATION, "token " + gitHubAccessToken).execute().returnResponse();
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, GithubConstants.GITHUB_ERROR,
                        "Non-200 Response from Github", "Status code from Github: " + Integer.toString(statusCode));
            }
            Map<String, Object> jsonData = CollectionUtils.toMap(jsonMapper.readValue(response.getEntity().getContent(), Map.class));

            String accountId = ObjectUtils.toString(jsonData.get("id"));
            String accountName = ObjectUtils.toString(jsonData.get(LOGIN));
            return new GithubAccountInfo(accountId, accountName);
        } catch (IOException e){
            logger.error(e);
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "GithubUnavailable", "Could not retrieve orgId from Github", null);
        }
    }

    public String getTeamOrgById(String id) {
        return  githubUtils.getTeamOrgById(id);
    }

    private void noGithub(Integer statusCode) {
        throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, GithubConstants.GITHUB_ERROR,
                "Non-200 Response from Github", "Status code from Github: " + Integer.toString(statusCode));
    }

}
