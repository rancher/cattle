package io.cattle.platform.iaas.api.filter.stack;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.StackDao;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.iaas.api.filter.common.CachedOutputFilter;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class StackOutputFilter extends CachedOutputFilter<Map<Long, List<Object>>> {

    @Inject
    StackDao stackDao;
    @Inject
    ObjectManager objectManager;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Stack.class };
    }

    @Override
    public String[] getTypes() {
        return new String[] {};
    }

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        if (request == null || "v1".equals(request.getVersion())) {
            return converted;
        }

        if (original instanceof Stack) {
            converted.getFields().put(ServiceConstants.STACK_FIELD_SERVICE_IDS,
                    getCached(request).get(((Stack) original).getId()));
        }
        return converted;
    }

    @Override
    protected Map<Long, List<Object>> newObject(ApiRequest apiRequest) {
        if (apiRequest == null) {
            return new HashMap<>();
        }
        List<Long> ids = getIds(apiRequest);
        return stackDao.getServicesForStack(ids, ApiContext.getContext().getIdFormatter());
    }

    @Override
    protected Long getId(Object obj) {
        if (obj instanceof Stack) {
            return ((Stack) obj).getId();
        }
        return null;
    }

}
