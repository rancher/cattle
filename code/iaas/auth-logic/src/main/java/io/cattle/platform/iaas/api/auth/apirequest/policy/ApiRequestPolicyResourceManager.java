package io.cattle.platform.iaas.api.auth.apirequest.policy;

import io.cattle.platform.api.auth.Policy;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;

import java.util.Collections;
import java.util.Map;

public class ApiRequestPolicyResourceManager extends AbstractNoOpResourceManager{
    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[]{ApiRequestPolicy.class};
    }

    @Override
    protected Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions
            options) {
        Policy policy = (Policy) ApiContext.getContext().getPolicy();
        ApiRequestPolicy apiRequestPolicy = new ApiRequestPolicy(policy);
        policy.grantObjectAccess(apiRequestPolicy);
        return Collections.singletonList(apiRequestPolicy);
    }
}
