package io.cattle.platform.iaas.api.manager;

import io.cattle.platform.api.resource.jooq.AbstractJooqResourceManager;
import io.cattle.platform.core.model.Service;
import io.github.ibuildthecloud.gdapi.model.Collection;
import io.github.ibuildthecloud.gdapi.model.impl.CollectionImpl;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.List;

public class ServiceManager extends AbstractJooqResourceManager {

    @Override
    public String[] getTypes() {
        return new String[] { "stack", "composeProject", "kubernetesStack" };
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Service.class };
    }

    @Override
    protected Collection createCollection(List<?> list, ApiRequest request) {
        Collection collection = super.createCollection(list, request);
        if (collection instanceof CollectionImpl) {
            if ("v1".equals(request.getVersion()) && collection instanceof CollectionImpl) {
                ((CollectionImpl) collection).setResourceType("environment");
            }
        }
        return collection;
    }

}
