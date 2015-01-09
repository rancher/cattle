package io.cattle.platform.iaas.api.auth.github;

import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;

import java.io.IOException;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class TokenResourceManager extends AbstractNoOpResourceManager {
    private static final String TOKEN = "token";

    private GithubTokenHandler githubTokenHandler;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Token.class };
    }

    @Override
    protected Object createInternal(String type, ApiRequest request) {
        if (!StringUtils.equals(TOKEN, request.getType())) {
            return null;
        }
        try {
            return githubTokenHandler.getGithubAccessToken(request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Inject
    public void setGithubTokenHandler(GithubTokenHandler githubTokenHandler) {
        this.githubTokenHandler = githubTokenHandler;
    }
}
