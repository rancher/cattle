package io.cattle.platform.iaas.api.auth;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.core.model.Account;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

public interface AuthorizationProvider {

    SchemaFactory getSchemaFactory(Account account, Policy policy, ApiRequest request);

    Policy getPolicy(Account account, ApiRequest request);

}
