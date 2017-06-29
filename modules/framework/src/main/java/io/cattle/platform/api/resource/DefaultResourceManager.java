package io.cattle.platform.api.resource;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.resource.jooq.JooqAccountAuthorization;
import io.cattle.platform.api.resource.jooq.JooqResourceListSupport;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.Map;

public class DefaultResourceManager implements ResourceManagerBaseSupport {

    protected ObjectResourceManagerSupport objectResourceManagerSupport;
    JooqResourceListSupport jooqResourceListSupport;
    JooqAccountAuthorization jooqAccountAuthorization;

    public DefaultResourceManager(DefaultResourceManagerSupport support) {
        super();
        this.objectResourceManagerSupport = support.getObjectResourceManagerSupport();
        this.jooqResourceListSupport = support.getJooqResourceListSupport();
        this.jooqAccountAuthorization = support.getJooqAccountAuthorization();
    }

    @Override
    public Object create(String type, ApiRequest request) {
        return objectResourceManagerSupport.create(type, request);
    }

    @Override
    public Object list(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        return jooqResourceListSupport.list(schemaFactory, type, criteria, options);
    }

    @Override
    public Object updateObject(String type, String id, Object obj, ApiRequest request) {
        return objectResourceManagerSupport.updateObject(type, id, obj, request);
    }

    @Override
    public Object deleteObject(String type, String id, Object obj, ApiRequest request) {
        return objectResourceManagerSupport.deleteObject(type, id, obj, request);
    }

    @Override
    public void addAccountAuthorization(boolean byId, boolean byLink, String type, Map<Object, Object> criteria, Policy policy) {
        jooqAccountAuthorization.addAccountAuthorization(byId, byLink, type, criteria, policy);
    }

}
