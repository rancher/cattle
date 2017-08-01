package io.cattle.platform.api.stack;

import io.cattle.platform.api.common.CachedOutputFilter;
import io.cattle.platform.core.constants.StackConstants;
import io.cattle.platform.core.dao.StackDao;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StackOutputFilter extends CachedOutputFilter<Map<Long, List<Object>>> {

    StackDao stackDao;
    ObjectManager objectManager;

    public StackOutputFilter(StackDao stackDao, ObjectManager objectManager) {
        super();
        this.stackDao = stackDao;
        this.objectManager = objectManager;
    }

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        if (request != null && original instanceof Stack) {
            converted.getFields().put(StackConstants.FIELD_SERVICE_IDS,
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
