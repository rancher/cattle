package io.cattle.platform.iaas.api.auth.identity;

import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.integration.github.GithubConstants;
import io.cattle.platform.iaas.api.auth.integration.interfaces.TokenCreator;
import io.cattle.platform.iaas.api.auth.TokenUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class TokenResourceManager extends AbstractNoOpResourceManager {

    private List<TokenCreator> tokenCreators;

    public List<TokenCreator> getTokenCreators() {
        return tokenCreators;
    }

    private static final Log logger = LogFactory.getLog(TokenResourceManager.class);

    @Inject
    public void setTokenCreators(List<TokenCreator> tokenCreators) {
        this.tokenCreators = tokenCreators;
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[]{Token.class};
    }

    @Override
    protected Object createInternal(String type, ApiRequest request) {
        if (!StringUtils.equals(TokenUtils.TOKEN, request.getType())) {
            return null;
        }
        return createToken(request);
    }

    private Token createToken(ApiRequest request) {
        Token token = null;

        for (TokenCreator tokenCreator : tokenCreators) {
            if (tokenCreator.isConfigured() && tokenCreator.providerType().equalsIgnoreCase(SecurityConstants.AUTH_PROVIDER.get())) {
                token = tokenCreator.createToken(request);
                break;
            }
        }
        if (token == null){
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE,
                    "codeTypeInvalid", "Code type provided invalid.", null);
        }
        return token;
    }

    @Override
    protected Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        //LEGACY: Used for old Implementation of projects/ Identities. Remove when vincent changes to new api.
        return new Token(SecurityConstants.SECURITY.get(), GithubConstants.GITHUB_CLIENT_ID.get(),
                GithubConstants.GITHUB_HOSTNAME.get(), SecurityConstants.AUTH_PROVIDER.get());
    }
}
