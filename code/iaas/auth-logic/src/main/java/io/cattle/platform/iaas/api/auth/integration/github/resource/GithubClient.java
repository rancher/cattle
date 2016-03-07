package io.cattle.platform.iaas.api.auth.integration.github.resource;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.iaas.api.auth.integration.github.GithubConfigurable;
import io.cattle.platform.iaas.api.auth.integration.github.GithubConstants;
import io.cattle.platform.iaas.api.auth.integration.github.GithubTokenUtil;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.message.BasicNameValuePair;

public class GithubClient extends GithubConfigurable{

    private static final String LINK = "link";
    private static final String RELATION = "rel";
    private static final String NEXT = "next";

    @Inject
    private GithubTokenUtil githubTokenUtils;

    @Inject
    private JsonMapper jsonMapper;

    private static final Log logger = LogFactory.getLog(GithubClient.class);

    private Identity getUserIdentity(String githubAccessToken) {
        if (StringUtils.isEmpty(githubAccessToken)) {
            noAccessToken();
        }
        try {
            HttpResponse response = getFromGithub(githubAccessToken, getURL(GithubClientEndpoints.USER_INFO));

            Map<String, Object> jsonData = jsonMapper.readValue(response.getEntity().getContent());
            return jsonToGithubAccountInfo(jsonData).toIdentity(GithubConstants.USER_SCOPE);
        } catch (IOException e) {
            logger.error("Failed to get Github user account info.", e);
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, GithubConstants.GITHUB_CLIENT,
                    "Failed to get Github user account info.", null);
        }
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

    public List<Identity> getOrgAccountInfo(String githubAccessToken) {
        try {
            if (StringUtils.isEmpty(githubAccessToken)) {
                noAccessToken();
            }
            List<Identity> orgs = new ArrayList<>();
            List<Map<String, Object>> jsonData;

            List<HttpResponse> responses = paginateGithub(githubAccessToken, getURL(GithubClientEndpoints.ORG_INFO));
            for (HttpResponse response :
                    responses) {
                jsonData = jsonMapper.readCollectionValue(response.getEntity().getContent(), List.class, Map.class);

                for (Map<String, Object> orgObject : jsonData) {
                    orgs.add(jsonToGithubAccountInfo(orgObject).toIdentity(GithubConstants.ORG_SCOPE));
                }
            }
            return orgs;
        }
        catch (IOException e){
            logger.error("Failed to get org account info.", e);
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, GithubConstants.GITHUB_CLIENT,
                    "Failed to get org account info.", null);
        }
    }

    private List<HttpResponse> paginateGithub(String githubAccessToken, String url) throws IOException {
        List<HttpResponse> responses = new ArrayList<>();
        HttpResponse response = getFromGithub(githubAccessToken, url);
        responses.add(response);
        String nextUrl = nextGithubPage(response);
        while (StringUtils.isNotBlank(nextUrl)) {
            response = getFromGithub(githubAccessToken, nextUrl);
            responses.add(response);
            nextUrl = nextGithubPage(response);
        }
        return responses;
    }

    private void noAccessToken() {
        throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR,
                "GithubAccessToken", "No github Access token", null);
    }

    public List<Identity> getTeamsInfo(String githubAccessToken) {
        try {
            if (StringUtils.isEmpty(githubAccessToken)) {
                noAccessToken();
            }

            List<HttpResponse> responses = paginateGithub(githubAccessToken, getURL(GithubClientEndpoints.TEAMS));
            ArrayList<Identity> teams = new ArrayList<>();
            for (HttpResponse response :
                    responses) {
                teams.addAll(getTeamInfo(response));
            }
            return teams;
        }
        catch (IOException e) {
            logger.error("Failed to get team account info.", e);
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, GithubConstants.GITHUB_CLIENT,
                    "Failed to get team account info.", null);
        }
    }

    private List<Identity> getTeamInfo(HttpResponse response) throws IOException {
        List<Identity> teams = new ArrayList<>();
        List<Map<String, Object>> jsonData;
        jsonData = jsonMapper.readCollectionValue(response.getEntity().getContent(), List.class, Map.class);
        for (Map<String, Object> teamObject : jsonData) {
            teams.add(getTeam(teamObject));
        }
        return teams;

    }

    public Identity getTeam(Map<String, Object> teamObject) {
        String accountId = ObjectUtils.toString(teamObject.get(GithubConstants.TEAM_ID));
        String teamName = ObjectUtils.toString(teamObject.get(GithubConstants.NAME_FIELD));
        String slug = ObjectUtils.toString(teamObject.get("slug"));
        Map<String, Object> org = CollectionUtils.toMap(teamObject.get("organization"));
        String orgLogin = ObjectUtils.toString(org.get(GithubConstants.LOGIN));
        String orgName = ObjectUtils.toString(org.get(GithubConstants.LOGIN));
        String profilePicture = ObjectUtils.toString(org.get(GithubConstants.PROFILE_PICTURE));
        String profileUrl = String.format(getURL(GithubClientEndpoints.TEAM_PROFILE), orgLogin, slug);
        return new Identity(GithubConstants.TEAM_SCOPE, accountId,
                StringUtils.isBlank(orgName) ? orgLogin : orgName + " : " + teamName,
                profileUrl, profilePicture, slug);
    }

    private String nextGithubPage(HttpResponse response) {
        if (response.getFirstHeader(LINK) != null) {
            for(HeaderElement element: response.getFirstHeader(LINK).getElements()) {
                if (element.getParameterByName(RELATION) != null &&
                        NEXT.equalsIgnoreCase(element.getParameterByName(RELATION).getValue())){
                    String next = String.valueOf(element).split(";")[0];
                    return next.substring(1, next.length() - 1);
                }
            }
        }
        return null;
    }

    public HttpResponse getFromGithub(String githubAccessToken, String url) throws IOException {

        HttpResponse response = Request.Get(url).addHeader(GithubConstants.AUTHORIZATION, "token " +
                "" + githubAccessToken).addHeader(GithubConstants.ACCEPT, GithubConstants.APPLICATION_JSON).execute().returnResponse();
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            noGithub(statusCode);
        }
        return response;
    }

    public GithubAccountInfo getGithubUserByName(String username) {
        String githubAccessToken = (String) ApiContext.getContext().getApiRequest().getAttribute(GithubConstants.GITHUB_ACCESS_TOKEN);
        if (StringUtils.isEmpty(githubAccessToken)) {
            noAccessToken();
        }
        try {
            if (StringUtils.isEmpty(username)) {
                throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR,
                        "getGithubUser", "No github username specified.", null);
            }
            username = URLEncoder.encode(username, "UTF-8");
            try {
                if (getGithubOrgByName(username) != null) {
                    return null;
                }
            } catch (ClientVisibleException ignored) {}
            HttpResponse response = getFromGithub(githubAccessToken, getURL(GithubClientEndpoints.USERS) + username);
            Map<String, Object> jsonData = CollectionUtils.toMap(jsonMapper.readValue(response.getEntity().getContent(), Map.class));
            return jsonToGithubAccountInfo(jsonData);
        } catch (IOException e) {
            logger.error(e);
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "GithubUnavailable", "Could not retrieve UserId from Github", null);
        }

    }

    public GithubAccountInfo getGithubOrgByName(String org) {
        String githubAccessToken = (String) ApiContext.getContext().getApiRequest().getAttribute(GithubConstants.GITHUB_ACCESS_TOKEN);
        if (StringUtils.isEmpty(githubAccessToken)) {
            noAccessToken();
        }
        try {
            if (StringUtils.isEmpty(org)) {
                throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR,
                        "noGithubOrgName", "No org name specified when retrieving from Github.", null);
            }
            org = URLEncoder.encode(org, "UTF-8");
            HttpResponse response = getFromGithub(githubAccessToken, getURL(GithubClientEndpoints.ORGS) + org);

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
                toReturn = apiEndpoint + "/user/orgs?per_page=100";
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
            case TEAMS:
                toReturn = apiEndpoint + "/user/teams?per_page=100";
                break;
            case TEAM_PROFILE:
                toReturn = hostName + "/orgs/%s/teams/%s";
                break;
            default:
                throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR,
                        "GithubClient", "Attempted to get invalid Api endpoint.", null);
        }
        return toReturn;
    }

    public GithubAccountInfo getUserOrgById(String id) {
        String githubAccessToken = (String) ApiContext.getContext().getApiRequest().getAttribute(GithubConstants.GITHUB_ACCESS_TOKEN);
        if (StringUtils.isEmpty(githubAccessToken)) {
            noAccessToken();
        }
        try {
            HttpResponse response = getFromGithub(githubAccessToken, getURL(GithubClientEndpoints.USER_INFO) + '/' + id);

            Map<String, Object> jsonData = CollectionUtils.toMap(jsonMapper.readValue(response.getEntity().getContent(), Map.class));
            return jsonToGithubAccountInfo(jsonData);
        } catch (IOException e) {
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "GithubUnavailable", "Could not retrieve UserId from Github", null);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchGithub(String url) {
        try {
            HttpResponse res = getFromGithub(githubTokenUtils.getAccessToken(), url);
            //TODO:Finish implementing search.
            Map<String, Object> jsonData = jsonMapper.readValue(res.getEntity().getContent());
            return (List<Map<String, Object>>) jsonData.get("items");
        } catch (IOException e) {
            //TODO: Proper Error Handling.
            return new ArrayList<>();
        }
    }

    @Override
    public String getName() {
        return GithubConstants.GITHUB_CLIENT;
    }

    public Set<Identity> getIdentities(String accessToken) {
        Set<Identity> identities = new HashSet<>();
        identities.add(getUserIdentity(accessToken));
        identities.addAll(getOrgAccountInfo(accessToken));
        identities.addAll(getTeamsInfo(accessToken));
        return identities;
    }
}
