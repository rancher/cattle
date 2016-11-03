package io.cattle.platform.iaas.api.filter.instance;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.iaas.api.filter.common.CachedOutputFilter;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class InstanceOutputFilter extends CachedOutputFilter<Map<Long, List<Object>>> {

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
            Map<Long, List<Object>> data = getCached(request);
            if (data != null) {
                converted.getFields().put(InstanceConstants.FIELD_SERVICE_IDS, data.get(((Instance) original).getId()));
            }
        }

        List<Long> networkIds = DataAccessor.fieldLongList(original, InstanceConstants.FIELD_NETWORK_IDS);
        if (networkIds.size() > 0) {
            IdFormatter idF = ApiContext.getContext().getIdFormatter();
            converted.getFields().put(InstanceConstants.FIELD_PRIMARY_NETWORK_ID, idF.formatId(NetworkConstants.KIND_NETWORK, networkIds.get(0)));
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
