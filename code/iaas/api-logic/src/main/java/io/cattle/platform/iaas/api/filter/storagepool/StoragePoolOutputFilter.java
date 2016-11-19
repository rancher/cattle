package io.cattle.platform.iaas.api.filter.storagepool;

import io.cattle.platform.core.constants.StoragePoolConstants;
import io.cattle.platform.core.dao.StoragePoolDao;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.iaas.api.filter.common.CachedOutputFilter;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class StoragePoolOutputFilter extends CachedOutputFilter<Map<Long, Map<String, Object>>> {

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
            Map<Long, Map<String, Object>> data = getCached(request);
            if (data == null) {
                return converted;
            }
            Map<String, Object> fields = data.get(((StoragePool) original).getId());
            if (fields != null) {
                converted.getFields().putAll(fields);
            }
        }
        return converted;
    }

    @Override
    protected Map<Long, Map<String, Object>> newObject(ApiRequest apiRequest) {
        Map<Long, Map<String, Object>> allFields = new HashMap<>();
        IdFormatter idF = ApiContext.getContext().getIdFormatter();
        List<Long> ids = getIds(apiRequest);
        Map<Long, List<Object>> hosts = storagePoolDao.findHostsForPools(ids, idF);
        Map<Long, List<Object>> volumes = storagePoolDao.findVolumesForPools(ids, idF);

        for (Map.Entry<Long, List<Object>> entry : hosts.entrySet()) {
            allFields.put(entry.getKey(), CollectionUtils.asMap(StoragePoolConstants.FIELD_HOST_IDS, entry.getValue()));
        }

        for (Map.Entry<Long, List<Object>> entry : volumes.entrySet()) {
            Map<String, Object> fields = allFields.get(entry.getKey());
            if (fields == null) {
                fields = new HashMap<>();
                allFields.put(entry.getKey(), fields);
            }
            fields.put(StoragePoolConstants.FIELD_VOLUME_IDS, entry.getValue());
        }

        return allFields;
    }

    @Override
    protected Long getId(Object obj) {
        if (obj instanceof StoragePool) {
            return ((StoragePool) obj).getId();
        }
        return null;
    }

}
