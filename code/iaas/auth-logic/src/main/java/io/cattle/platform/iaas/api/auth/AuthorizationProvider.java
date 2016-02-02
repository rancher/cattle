package io.cattle.platform.iaas.api.auth;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.core.model.Account;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.Set;

public interface AuthorizationProvider {

    SchemaFactory getSchemaFactory(Account account, Policy policy, ApiRequest request);

    String getRole(Account account, Policy policy, ApiRequest request);

    Policy getPolicy(Account account, Account authenticatedAsAccount, Set<Identity> identities, ApiRequest request);

}
