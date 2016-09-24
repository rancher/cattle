package io.cattle.platform.iaas.api.filter.instance;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.iaas.api.filter.common.CachedOutputFilter;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class InstanceOutputFilter extends CachedOutputFilter<Map<Long,List<Object>>> {

    @Inject
    ServiceDao serviceDao;
    @Inject
    ObjectManager objectManager;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] {};
    }

    @Override
    public String[] getTypes() {
        return new String[]{InstanceConstants.TYPE_CONTAINER};
    }

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        if (original instanceof Instance) {
            converted.getFields().put(InstanceConstants.FIELD_SERVICE_IDS,
                    getCached(request).get(((Instance) original).getId()));
        }
        return converted;
    }

    @Override
    protected Map<Long, List<Object>> newObject(ApiRequest apiRequest) {
        return serviceDao.getServicesForInstances(getIds(apiRequest), ApiContext.getContext().getIdFormatter());
    }

    @Override
    protected Long getId(Object obj) {
        if (obj instanceof Instance) {
            return ((Instance) obj).getId();
        }
        return null;
    }



}
