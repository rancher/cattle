package io.cattle.platform.api.resource;

import io.cattle.platform.api.auth.Policy;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.Map;

public abstract class AbstractNoOpResourceManager implements ResourceManagerBaseSupport {

    @Override
    public Object create(String type, ApiRequest request) {
        return null;
    }

    @Override
    public Object updateObject(String type, String id, Object obj, ApiRequest request) {
        return null;
    }

    @Override
    public Object list(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        return null;
    }

    @Override
    public Object deleteObject(String type, String id, Object obj, ApiRequest request) {
        return null;
    }

    @Override
    public void addAccountAuthorization(boolean byId, boolean byLink, String type, Map<Object, Object> criteria, Policy policy) {
    }

}
