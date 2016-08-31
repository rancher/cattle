package io.cattle.platform.iaas.api.auth.integration.github;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.constants.IdentityConstants;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.AuthToken;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.dao.AuthTokenDao;
import io.cattle.platform.iaas.api.auth.integration.github.resource.GithubAccountInfo;
import io.cattle.platform.iaas.api.auth.integration.github.resource.GithubClient;
import io.cattle.platform.iaas.api.auth.integration.github.resource.GithubClientEndpoints;
import io.cattle.platform.iaas.api.auth.integration.interfaces.IdentityProvider;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;

public class GithubIdentityProvider extends GithubConfigurable implements IdentityProvider {

    @Inject
    GithubClient githubClient;

    @Inject
    private JsonMapper jsonMapper;
    @Inject
    private GithubTokenUtil githubTokenUtils;
    @Inject
    private AuthTokenDao authTokenDao;
    @Inject
    GithubTokenCreator githubTokenCreator;

    private static final Log logger = LogFactory.getLog(GithubIdentityProvider.class);


    @Override
    public List<Identity> searchIdentities(String name, boolean exactMatch) {
        if (!isConfigured()){
            notConfigured();
        }
        List<Identity> identities = new ArrayList<>();
        for (String scope : scopes()) {
            identities.addAll(searchIdentities(name, scope, exactMatch));
        }
        return identities;
    }

    @Override
    public List<Identity> searchIdentities(String name, String scope, boolean exactMatch) {
        //TODO:Implement exact match.
        if (!isConfigured()){
            notConfigured();
        }
        switch (scope){
            case GithubConstants.USER_SCOPE:
                return searchUsers(name, exactMatch);
            case GithubConstants.ORG_SCOPE:
                return searchGroups(name, exactMatch);
            default:
                return new ArrayList<>();
        }
    }

    @Override
    public Set<Identity> getIdentities(Account account) {
        if (!isConfigured() || !GithubConstants.CONFIG.equalsIgnoreCase(SecurityConstants.AUTH_PROVIDER.get()) ||
                !GithubConstants.USER_SCOPE.equalsIgnoreCase(account.getExternalIdType())) {
            return new HashSet<>();
        }
        ApiRequest request = ApiContext.getContext().getApiRequest();
        String accessToken = (String) DataAccessor.fields(account).withKey(GithubConstants.GITHUB_ACCESS_TOKEN).get();
        request.setAttribute(GithubConstants.GITHUB_ACCESS_TOKEN, accessToken);
        if (githubTokenUtils.findAndSetJWT()){
            request.setAttribute(GithubConstants.GITHUB_ACCESS_TOKEN, accessToken);
            return githubTokenUtils.getIdentities();
        }
        String jwt = null;
        if (!StringUtils.isBlank(accessToken) && SecurityConstants.SECURITY.get()) {
            AuthToken authToken = authTokenDao.getTokenByAccountId(account.getId());
            if (authToken == null) {
                try {
                    jwt = ProjectConstants.AUTH_TYPE + githubTokenCreator.getGithubToken(accessToken).getJwt();
                    authToken = authTokenDao.createToken(jwt, GithubConstants.CONFIG, account.getId());
                    jwt = authToken.getKey();
                } catch (ClientVisibleException e) {
                    if (e.getCode().equalsIgnoreCase(GithubConstants.GITHUB_ERROR) &&
                            !e.getDetail().contains("401")) {
                        throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR,
                                GithubConstants.JWT_CREATION_FAILED, "", null);
                    }
                }
            } else {
                jwt = authToken.getKey();
            }

        }
        if (StringUtils.isBlank(jwt)){
            return Collections.emptySet();
        }
        request.setAttribute(GithubConstants.GITHUB_JWT, jwt);
        return githubTokenUtils.getIdentities();
    }

    private List<Identity> searchGroups(String groupName, boolean exactMatch) {
        List<Identity> identities = new ArrayList<>();
        if (exactMatch) {
            GithubAccountInfo group;
            try {
                group =  githubClient.getGithubOrgByName(groupName);
                if (group == null){
                    return identities;
                }
            } catch (ClientVisibleException e) {
                return identities;
            }
            Identity identity = group.toIdentity(GithubConstants.ORG_SCOPE);
            identities.add(identity);
            return identities;
        }
        String url;
        try {
            url = githubClient.getURL(GithubClientEndpoints.USER_SEARCH) + URLEncoder.encode(groupName, "UTF-8") +
                        "+type:org";
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        List<Map<String, Object>> results = githubClient.searchGithub(url);
        for (Map<String, Object> user: results){
            identities.add(githubClient.jsonToGithubAccountInfo(user).toIdentity(GithubConstants.ORG_SCOPE));
        }
        return identities;
    }

    private List<Identity> searchUsers(String userName, boolean exactMatch) {
        List<Identity> identities = new ArrayList<>();
        if (exactMatch) {
            GithubAccountInfo user;
            try {
                user =  githubClient.getGithubUserByName(userName);
            } catch (ClientVisibleException e) {
                return identities;
            }
            if (user == null) {
                return identities;
            }
            Identity identity = user.toIdentity(GithubConstants.USER_SCOPE);
            identities.add(identity);
            return identities;
        }
        String url = null;
        try {
            url = githubClient.getURL(GithubClientEndpoints.USER_SEARCH) + URLEncoder.encode(userName, "UTF-8") +
                        "+type:user";
        } catch (UnsupportedEncodingException e) {
            logger.error(e);
        }
        List<Map<String, Object>> results = githubClient.searchGithub(url);
        for (Map<String, Object> user: results){
            identities.add(githubClient.jsonToGithubAccountInfo(user).toIdentity(GithubConstants.USER_SCOPE));
        }
        return identities;

    }

    @Override
    public Identity getIdentity(String id, String scope) {
        if (!isConfigured()){
            notConfigured();
        }
        switch (scope) {
            case GithubConstants.USER_SCOPE:
                GithubAccountInfo user = githubClient.getUserOrgById(id);
                return user.toIdentity(GithubConstants.USER_SCOPE);
            case GithubConstants.ORG_SCOPE:
                GithubAccountInfo org = githubClient.getUserOrgById(id);
                return org.toIdentity(GithubConstants.ORG_SCOPE);
            case GithubConstants.TEAM_SCOPE:
                return getTeamById(id);
            default:
                throw new ClientVisibleException(ResponseCodes.BAD_REQUEST,
                        IdentityConstants.INVALID_TYPE, "Invalid scope for GithubSearchProvider", null);
        }
    }

    private void notConfigured() {
        throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE,
                "NotConfigured", "Github is not configured", null);
    }

    private Identity getTeamById(String id) {
        if (!isConfigured()) {
            notConfigured();
        }
        String gitHubAccessToken = (String) ApiContext.getContext().getApiRequest().getAttribute(GithubConstants.GITHUB_ACCESS_TOKEN);
        try {
            if (StringUtils.isEmpty(id)) {
                return null;
            }
            HttpResponse response = githubClient.getFromGithub(gitHubAccessToken, githubClient.getURL(GithubClientEndpoints.TEAM) + id);

            Map<String, Object> teamData = CollectionUtils.toMap(jsonMapper.readValue(response.getEntity().getContent
                    (), Map.class));

            return githubClient.getTeam(teamData);
        } catch (IOException e) {
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "GithubUnavailable", "Could not retrieve orgId from Github", null);
        }
    }

    @Override
    public Set<String> scopes() {
        return GithubConstants.SCOPES;
    }

    @Override
    public String getName() {
        return GithubConstants.SEARCH_PROVIDER;
    }

    public List<Identity> savedIdentities() {
        List<String> ids = githubTokenUtils.fromCommaSeparatedString(GithubConstants.GITHUB_ALLOWED_IDENTITIES.get());
        List<Identity> identities = new ArrayList<>();
        if (ids.isEmpty() || !isConfigured()) {
            return identities;
        }
        for(String id: ids){
            String[] split = id.split(":", 2);
            identities.add(getIdentity(split[1], split[0]));
        }
        return identities;
    }

    @Override
    public Identity transform(Identity identity) {
        if (scopes().contains(identity.getExternalIdType())) {
            return getIdentity(identity.getExternalId(), identity.getExternalIdType());
        }
        throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, IdentityConstants.INVALID_TYPE,
            "Github does not provide: " + identity.getExternalIdType(), null );
    }

    @Override
    public Identity untransform(Identity identity) {
        if (scopes().contains(identity.getExternalIdType())) {
            return identity;
        }
        throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, IdentityConstants.INVALID_TYPE,
            "Github does not provide: " + identity.getExternalIdType(), null );
    }
}
