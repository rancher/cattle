package io.cattle.platform.iaas.api.auth.integration.azure;

import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.identity.Token;
import io.cattle.platform.iaas.api.auth.integration.interfaces.TokenCreator;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

public class AzureTokenCreator extends AzureConfigurable implements TokenCreator {

    @Inject
    AzureTokenUtil azureTokenUtils;
    @Inject
    AzureRESTClient azureClient;

    public Token getAzureToken(String accessToken) {
        if (!isConfigured()) {
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, AzureConstants.CONFIG, "No Azure Client id found.", null);
        }
        return azureTokenUtils.createToken(azureClient.getIdentities(accessToken), null);
    }

    @Override
    public Token getToken(ApiRequest request) {
        Map<String, Object> requestBody = CollectionUtils.toMap(request.getRequestObject());
        String code = ObjectUtils.toString(requestBody.get(SecurityConstants.CODE));
        String accessToken = azureClient.getAccessToken(code);
        if (StringUtils.isBlank(accessToken)){
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, getName(), "Failed to get accessToken.",
                    null);
        }
        request.setAttribute(AzureConstants.AZURE_ACCESS_TOKEN, accessToken);
        return getAzureToken(accessToken);
    }

    @Override
    public void reset() {

    }

    @Override
    public String getName() {
        return AzureConstants.TOKEN_CREATOR;
    }
}
