package io.cattle.platform.iaas.api.auth.integration.github;

import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.identity.Token;
import io.cattle.platform.iaas.api.auth.integration.github.resource.GithubClient;
import io.cattle.platform.iaas.api.auth.integration.interfaces.TokenCreator;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

public class GithubTokenCreator extends GithubConfigurable implements TokenCreator {

    @Inject
    GithubTokenUtil githubTokenUtils;
    @Inject
    GithubClient githubClient;

    public Token getGithubToken(String accessToken) {
        if (!isConfigured()) {
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, GithubConstants.CONFIG, "No Github Client id and secret found.", null);
        }
        return githubTokenUtils.createToken(githubClient.getIdentities(accessToken), null);
    }

    @Override
    public Token getToken(ApiRequest request) {
        Map<String, Object> requestBody = CollectionUtils.toMap(request.getRequestObject());
        String code = ObjectUtils.toString(requestBody.get(SecurityConstants.CODE));
        String accessToken = githubClient.getAccessToken(code);
        if (StringUtils.isBlank(accessToken)){
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, getName(), "Failed to get accessToken.",
                    null);
        }
        request.setAttribute(GithubConstants.GITHUB_ACCESS_TOKEN, accessToken);
        return getGithubToken(accessToken);
    }

    @Override
    public String getName() {
        return GithubConstants.TOKEN_CREATOR;
    }
}
