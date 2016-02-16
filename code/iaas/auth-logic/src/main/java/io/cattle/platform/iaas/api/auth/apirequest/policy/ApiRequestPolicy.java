package io.cattle.platform.iaas.api.auth.apirequest.policy;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.api.auth.Policy;
import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;
import io.github.ibuildthecloud.gdapi.model.FieldType;

import java.util.Set;

@Type(name = "apiRequestPolicy")
public class ApiRequestPolicy {
    private Policy policy;

    public ApiRequestPolicy(Policy policy) {
        this.policy = policy;
    }

    @Field(type = FieldType.ARRAY)
    public Set<Identity> getIdentities() {
        return this.policy.getIdentities();
    }

    @Field(type = FieldType.INT)
    public long getAccountId() {
        return policy.getAccountId();
    }

    @Field(type = FieldType.INT)
    public long getAuthenticatedAsAccountId() {
        return policy.getAuthenticatedAsAccountId();
    }

    @Field(type = FieldType.STRING)
    public String getUsername() {
        return policy.getUserName();
    }
}
