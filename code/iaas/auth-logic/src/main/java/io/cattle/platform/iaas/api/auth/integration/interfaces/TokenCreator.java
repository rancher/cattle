package io.cattle.platform.iaas.api.auth.integration.interfaces;

import io.cattle.platform.iaas.api.auth.identity.Token;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

public interface TokenCreator extends Configurable, Provider{

    Token getToken(ApiRequest request);

    void reset();
}
