package io.github.ibuildthecloud.dstack.iaas.api.auth;

import io.github.ibuildthecloud.dstack.api.auth.Policy;
import io.github.ibuildthecloud.dstack.core.model.Account;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

public interface AuthorizationProvider {

    SchemaFactory getSchemaFactory(Account account, ApiRequest request);

    Policy getPolicy(Account account, ApiRequest request);

}
