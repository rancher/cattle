package io.cattle.platform.iaas.api.auth.github;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.iaas.api.auth.TokenHandler;
import io.cattle.platform.iaas.api.auth.github.resource.Token;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringProperty;

public class TokenResourceManager extends AbstractNoOpResourceManager {
    private static final String TOKEN = "token";
    private static final DynamicBooleanProperty SECURITY = ArchaiusUtil.getBoolean("api.security.enabled");
    private static final DynamicStringProperty GITHUB_CLIENT_ID = ArchaiusUtil.getString("api.auth.github.client.id");
    private static final DynamicStringProperty GITHUB_HOSTNAME = ArchaiusUtil.getString("api.github.domain");

    public List<TokenHandler> getTokenHandlers() {
        return tokenHandlers;
    }

    @Inject
    public void setTokenHandlers(List<TokenHandler> tokenHandlers) {
        this.tokenHandlers = tokenHandlers;
    }

    private List<TokenHandler> tokenHandlers;

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
            return getToken(request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Token getToken(ApiRequest request) throws IOException {
        Token token = null;
        for (TokenHandler tokenHandler: tokenHandlers){
            token = tokenHandler.getToken(request);
            if (token != null){
                break;
            }
        }
        return token;
    }

    @Override
    protected Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        return new Token(SECURITY.get(), GITHUB_CLIENT_ID.get(), GITHUB_HOSTNAME.get());
    }
}
