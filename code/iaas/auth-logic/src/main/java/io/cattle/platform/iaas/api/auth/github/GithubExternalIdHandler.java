package io.cattle.platform.iaas.api.auth.github;

import io.cattle.platform.api.auth.ExternalId;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.ExternalIdHandler;
import io.cattle.platform.iaas.api.auth.github.constants.GithubConstants;
import io.cattle.platform.iaas.api.auth.github.resource.GithubAccountInfo;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class GithubExternalIdHandler implements ExternalIdHandler {


    @Inject
    GithubClient githubClient;
    @Inject
    GithubUtils githubUtils;
    @Inject
    GithubTokenHandler githubTokenHandler;

    @Override
    public ExternalId transform(ExternalId externalId) {
        String id;
        String name;
        GithubAccountInfo githubAccountInfo;
        switch (externalId.getType()){
            case GithubConstants.USER_SCOPE:
                githubAccountInfo = githubClient.getUserIdByName(externalId.getId());
                id = githubAccountInfo.getAccountId();
                name = githubAccountInfo.getAccountName();
                return new ExternalId(id, GithubConstants.USER_SCOPE, name);
            case GithubConstants.ORG_SCOPE:
                    githubAccountInfo = githubClient.getOrgIdByName(externalId.getId());
                    id = githubAccountInfo.getAccountId();
                    name = githubAccountInfo.getAccountName();
                return new ExternalId(id, GithubConstants.ORG_SCOPE, name);
            case GithubConstants.TEAM_SCOPE:
                    String org = githubClient.getTeamOrgById(externalId.getId());
                    return new ExternalId(externalId.getId(), GithubConstants.TEAM_SCOPE, org);
            default:
                return null;
        }
    }

    @Override
    public ExternalId untransform(ExternalId externalId) {
        switch (externalId.getType()) {
            case GithubConstants.USER_SCOPE:
                return new ExternalId(externalId.getName(), externalId.getType(), externalId.getName());
            case GithubConstants.ORG_SCOPE:
                return new ExternalId(externalId.getName(), externalId.getType(), externalId.getName());
            case GithubConstants.TEAM_SCOPE:
                return externalId;
            default:
                return null;
        }
    }

    @Override
    public Set<ExternalId> getExternalIds(Account account) {
        ApiRequest request = ApiContext.getContext().getApiRequest();
        githubUtils.findAndSetJWT();
        String jwt = githubUtils.getJWT();
        String accessToken = (String) DataAccessor.fields(account).withKey(GithubConstants.GITHUB_ACCESS_TOKEN).get();
        if(StringUtils.isBlank(jwt) && !StringUtils.isBlank(accessToken)){
            try {
                jwt =  ProjectConstants.AUTH_TYPE + githubTokenHandler.getGithubToken(accessToken).getJwt();
            } catch (IOException e) {
                throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "FailedToMakeJWT", "", null);
            }
        }
        if ( jwt != null && !jwt.isEmpty()){
            request.setAttribute(GithubConstants.GITHUB_JWT, jwt);
            request.setAttribute(GithubConstants.GITHUB_ACCESS_TOKEN, accessToken);
            return githubUtils.getExternalIds();
        }
        return new HashSet<>();
    }
}
