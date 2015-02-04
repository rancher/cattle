package io.cattle.platform.iaas.api.auth.github;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.iaas.api.auth.github.resource.Token;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringProperty;

public class TokenResourceManager extends AbstractNoOpResourceManager {
    private static final String TOKEN = "token";
    private static final DynamicBooleanProperty SECURITY = ArchaiusUtil.getBoolean("api.security.enabled");
    private static final DynamicStringProperty GITHUB_CLIENT_ID = ArchaiusUtil.getString("api.auth.github.client.id");

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

    @Override
    protected Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        return new Token(null, null, null, null, SECURITY.get(), GITHUB_CLIENT_ID.get());
    }

    @Inject
    public void setGithubTokenHandler(GithubTokenHandler githubTokenHandler) {
        this.githubTokenHandler = githubTokenHandler;
    }
}
