package io.cattle.platform.iaas.api.auth.integration.github.resource;

import io.cattle.platform.iaas.api.auth.integration.github.GithubConfigurable;
import io.cattle.platform.iaas.api.auth.integration.github.GithubConstants;
import io.cattle.platform.iaas.api.auth.integration.github.GithubUtils;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;
import java.net.URLEncoder;
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

public class GithubClient extends GithubConfigurable{

    @Inject
    private GithubUtils githubUtils;

    @Inject
    private JsonMapper jsonMapper;

    private static final Log logger = LogFactory.getLog(GithubClient.class);

    public GithubAccountInfo getUserAccountInfo(String githubAccessToken) {
        if (StringUtils.isEmpty(githubAccessToken)) {
            noAccessToken();
        }
        Map<String, Object> jsonData;

        HttpResponse response;
        try {
            response = Request.Get(getURL(GithubClientEndpoints.USER_INFO))
                    .addHeader(GithubConstants.AUTHORIZATION, "token " + githubAccessToken)
                    .addHeader(GithubConstants.ACCEPT, GithubConstants.APPLICATION_JSON).execute().returnResponse();
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                noGithub(statusCode);
            }
            jsonData = jsonMapper.readValue(response.getEntity().getContent());
        } catch (IOException e) {
            logger.error("Failed to get user account info.", e);
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, GithubConstants.GITHUB_CLIENT,
                    "Failed to get user account info.", null);
        }
        return jsonToGithubAccountInfo(jsonData);
    }

    public String getAccessToken(String code) {
        List<BasicNameValuePair> requestData = new ArrayList<>();

        if (!isConfigured()) {
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, GithubConstants.CONFIG, "No Github Client id and secret found.", null);
        }

        requestData.add(new BasicNameValuePair(GithubConstants.CLIENT_ID, GithubConstants.GITHUB_CLIENT_ID.get()));
        requestData.add(new BasicNameValuePair(GithubConstants.CLIENT_SECRET, GithubConstants.GITHUB_CLIENT_SECRET.get()));
        requestData.add(new BasicNameValuePair(GithubConstants.GITHUB_REQUEST_CODE, code));

        Map<String, Object> jsonData;

        HttpResponse response;
        try {
            response = Request.Post(getURL(GithubClientEndpoints.TOKEN))
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

            return (String) jsonData.get(GithubConstants.ACCESS_TOKEN);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public GithubAccountInfo jsonToGithubAccountInfo(Map<String, Object> jsonData) {
        String accountId = ObjectUtils.toString(jsonData.get("id"));
        String accountName = ObjectUtils.toString(jsonData.get(GithubConstants.LOGIN));
        String name = ObjectUtils.toString(jsonData.get(GithubConstants.NAME_FIELD));
        if (StringUtils.isBlank(name)) {
            name = accountName;
        }
        String profilePicture = ObjectUtils.toString(jsonData.get(GithubConstants.PROFILE_PICTURE));
        String profileUrl = ObjectUtils.toString(jsonData.get(GithubConstants.PROFILE_URL));
        return new GithubAccountInfo(accountId, accountName, profilePicture, profileUrl, name);
    }

    public List<GithubAccountInfo> getOrgAccountInfo(String githubAccessToken) {
        try {
            if (StringUtils.isEmpty(githubAccessToken)) {
                noAccessToken();
            }
            List<GithubAccountInfo> orgInfoList = new ArrayList<>();
            List<Map<String, Object>> jsonData;

            HttpResponse response = Request.Get(getURL(GithubClientEndpoints.ORG_INFO))
                    .addHeader(GithubConstants.AUTHORIZATION, "token " + githubAccessToken)
                    .addHeader(GithubConstants.ACCEPT, GithubConstants.APPLICATION_JSON).execute().returnResponse();

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                noGithub(statusCode);
            }
            jsonData = jsonMapper.readCollectionValue(response.getEntity().getContent(), List.class, Map.class);

            for (Map<String, Object> orgObject : jsonData) {
                orgInfoList.add(jsonToGithubAccountInfo(orgObject));
            }
            return orgInfoList;
        }
        catch (IOException e){
            logger.error("Failed to get org account info.", e);
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, GithubConstants.GITHUB_CLIENT,
                    "Failed to get org account info.", null);
        }
    }

    private void noAccessToken() {
        throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR,
                "GithubAccessToken", "No github Access token", null);
    }

    public List<TeamAccountInfo> getOrgTeamInfo(String githubAccessToken, String org) {
        try {
            if (StringUtils.isEmpty(githubAccessToken)) {
                noAccessToken();
            }
            List<TeamAccountInfo> teamInfoList = new ArrayList<>();
            List<Map<String, Object>> jsonData;

            HttpResponse response = Request.Get(getURL(GithubClientEndpoints.ORGS) + org + "/teams").addHeader(GithubConstants.AUTHORIZATION, "token " +
                    "" + githubAccessToken).addHeader(GithubConstants.ACCEPT, GithubConstants.APPLICATION_JSON).execute().returnResponse();
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                noGithub(statusCode);
            }
            jsonData = jsonMapper.readCollectionValue(response.getEntity().getContent(), List.class, Map.class);

            for (Map<String, Object> orgObject : jsonData) {
                String accountId = ObjectUtils.toString(orgObject.get("id"));
                String accountName = ObjectUtils.toString(orgObject.get(GithubConstants.NAME_FIELD));
                String slug = ObjectUtils.toString(orgObject.get("slug"));
                if (!StringUtils.equalsIgnoreCase("Owners", accountName)) {
                    teamInfoList.add(new TeamAccountInfo(org, accountName, accountId, slug));
                }
            }
            return teamInfoList;
        }
        catch (IOException e) {
            logger.error("Failed to get team account info.", e);
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, GithubConstants.GITHUB_CLIENT,
                    "Failed to get team account info.", null);
        }
    }

    public GithubAccountInfo getGithubUserByName(String username) {
        String githubAccessToken = (String) ApiContext.getContext().getApiRequest().getAttribute(GithubConstants.GITHUB_ACCESS_TOKEN);
        try {
            if (StringUtils.isEmpty(username)) {
                throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR,
                        "getGithubUser", "No github username specified.", null);
            }
            username = URLEncoder.encode(username, "UTF-8");
            if (getGithubOrgByName(username) != null){
                return null;
            }
            HttpResponse response = Request.Get(getURL(GithubClientEndpoints.USERS) + username)
                    .addHeader(GithubConstants.ACCEPT, GithubConstants.APPLICATION_JSON).addHeader(
                            GithubConstants.AUTHORIZATION, "token " + githubAccessToken).execute().returnResponse();
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                noGithub(statusCode);
            }
            Map<String, Object> jsonData = CollectionUtils.toMap(jsonMapper.readValue(response.getEntity().getContent(), Map.class));
            return jsonToGithubAccountInfo(jsonData);
        } catch (IOException e) {
            logger.error(e);
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "GithubUnavailable", "Could not retrieve UserId from Github", null);
        }

    }

    public GithubAccountInfo getGithubOrgByName(String org) {
        String gitHubAccessToken = (String) ApiContext.getContext().getApiRequest().getAttribute(GithubConstants.GITHUB_ACCESS_TOKEN);
        try {
            if (StringUtils.isEmpty(org)) {
                throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR,
                        "noGithubOrgName", "No org name specified when retrieving from Github.", null);
            }
            org = URLEncoder.encode(org, "UTF-8");;
            HttpResponse response = Request.Get(getURL(GithubClientEndpoints.ORGS) + org)
                    .addHeader(GithubConstants.ACCEPT, GithubConstants.APPLICATION_JSON)
                    .addHeader(GithubConstants.AUTHORIZATION, "token " + gitHubAccessToken).execute().returnResponse();
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                if (statusCode == 404) {
                    return null;
                }
                noGithub(statusCode);
            }
            Map<String, Object> jsonData = CollectionUtils.toMap(jsonMapper.readValue(response.getEntity().getContent
                    (), Map.class));
            return jsonToGithubAccountInfo(jsonData);
        } catch (IOException e) {
            logger.error(e);
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "GithubUnavailable", "Could not retrieve orgId from Github", null);
        }
    }

    public void noGithub(Integer statusCode) {
        throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, GithubConstants.GITHUB_ERROR,
                "Non-200 Response from Github", "Status code from Github: " + Integer.toString(statusCode));
    }

    public String getURL(GithubClientEndpoints val) {
        String hostName;
        String apiEndpoint;
        if (StringUtils.isBlank(GithubConstants.GITHUB_HOSTNAME.get())) {
            hostName = GithubConstants.GITHUB_DEFAULT_HOSTNAME;
            apiEndpoint = GithubConstants.GITHUB_API;
        } else {
            hostName = GithubConstants.SCHEME.get() + GithubConstants.GITHUB_HOSTNAME.get();
            apiEndpoint = GithubConstants.SCHEME.get() + GithubConstants.GITHUB_HOSTNAME.get() + GithubConstants.GHE_API;
        }
        String toReturn;
        switch (val) {
            case API:
                toReturn = apiEndpoint;
                break;
            case TOKEN:
                toReturn = hostName + "/login/oauth/access_token";
                break;
            case USERS:
                toReturn = apiEndpoint + "/users/";
                break;
            case ORGS:
                toReturn = apiEndpoint + "/orgs/";
                break;
            case USER_INFO:
                toReturn = apiEndpoint + "/user";
                break;
            case ORG_INFO:
                toReturn = apiEndpoint + "/user/orgs";
                break;
            case USER_PICTURE:
                toReturn = "https://avatars.githubusercontent.com/u/" + val + "?v=3&s=72";
                break;
            case USER_SEARCH:
                toReturn = apiEndpoint + "/search/users?q=";
                break;
            case TEAM:
                toReturn = apiEndpoint + "/teams/";
                break;
            default:
                throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR,
                        "GithubClient", "Attempted to get invalid Api endpoint.", null);
        }
        return toReturn;
    }

    public GithubAccountInfo getUserOrgById(String id) {
        String githubAccessToken = (String) ApiContext.getContext().getApiRequest().getAttribute(GithubConstants.GITHUB_ACCESS_TOKEN);
        try {
            HttpResponse response = Request.Get(getURL(GithubClientEndpoints.USER_INFO) + '/' + id)
                    .addHeader(GithubConstants.ACCEPT, GithubConstants.APPLICATION_JSON).addHeader(
                            GithubConstants.AUTHORIZATION, "token " + githubAccessToken).execute().returnResponse();
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                noGithub(statusCode);
            }
            Map<String, Object> jsonData = CollectionUtils.toMap(jsonMapper.readValue(response.getEntity().getContent(), Map.class));
            return jsonToGithubAccountInfo(jsonData);
        } catch (IOException e) {
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "GithubUnavailable", "Could not retrieve UserId from Github", null);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchGithub(String url) {
        try {
            HttpResponse res = Request.Get(url)
                    .addHeader("Authorization", "token " + githubUtils.getAccessToken()).addHeader
                            ("Accept", "application/json").execute().returnResponse();
            int statusCode = res.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                noGithub(statusCode);
            }
            //TODO:Finish implementing search.
            Map<String, Object> jsonData = jsonMapper.readValue(res.getEntity().getContent());
            return (List<Map<String, Object>>) jsonData.get("items");
        } catch (IOException e) {
            //TODO: Propper Error Handling.
            return new ArrayList<>();
        }
    }

    @Override
    public String getName() {
        return GithubConstants.GITHUB_CLIENT;
    }
}
