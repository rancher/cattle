package io.cattle.platform.iaas.api.filter.storagepool;

import io.cattle.platform.core.constants.StoragePoolConstants;
import io.cattle.platform.core.dao.StoragePoolDao;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.iaas.api.filter.common.CachedOutputFilter;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class StoragePoolOutputFilter extends CachedOutputFilter<Map<Long, List<Object>>> {

    @Inject
    StoragePoolDao storagePoolDao;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { StoragePool.class };
    }

    @Override
    public String[] getTypes() {
        return new String[] {StoragePoolConstants.TYPE};
    }

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        if (request != null && "v1".equals(request.getVersion())) {
            return converted;
        }

        if (original instanceof StoragePool) {
            Map<Long, List<Object>> data = getCached(request);
            if (data == null) {
                return converted;
            }
            converted.getFields().put(StoragePoolConstants.FIELD_HOST_IDS, data.get(((StoragePool) original).getId()));
        }
        return converted;
    }

    @Override
    protected Map<Long, List<Object>> newObject(ApiRequest apiRequest) {
        List<Long> ids = getIds(apiRequest);
        return storagePoolDao.findHostsForPools(ids, ApiContext.getContext().getIdFormatter());
    }

    @Override
    protected Long getId(Object obj) {
        if (obj instanceof StoragePool) {
            return ((StoragePool) obj).getId();
        }
        return null;
    }

}
