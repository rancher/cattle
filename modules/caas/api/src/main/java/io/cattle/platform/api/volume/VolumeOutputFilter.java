package io.cattle.platform.api.volume;

import io.cattle.platform.api.common.CachedOutputFilter;
import io.cattle.platform.core.addon.MountEntry;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.VolumeDao;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VolumeOutputFilter extends CachedOutputFilter<Map<Long, Map<String, Object>>> {

    ObjectManager objectManager;
    VolumeDao volumeDao;

    public VolumeOutputFilter(ObjectManager objectManager, VolumeDao volumeDao) {
        super();
        this.objectManager = objectManager;
        this.volumeDao = volumeDao;
    }

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        if (original instanceof Volume) {
            Map<Long, Map<String, Object>> data = getCached(request);
            if (data != null) {
                Map<String, Object> fields = data.get(((Volume) original).getId());
                if (fields != null) {
                    converted.getFields().putAll(fields);
                }
            }
        }

        return converted;
    }

    @Override
    protected Map<Long, Map<String, Object>> newObject(ApiRequest apiRequest) {
        Map<Long, Map<String, Object>> result = new HashMap<>();
        List<Long> ids = getIds(apiRequest);
        IdFormatter idF = ApiContext.getContext().getIdFormatter();

        for (Map.Entry<Long, Collection<MountEntry>> entry : volumeDao.getMountsForVolumes(ids, idF).entrySet()) {
            Map<String, Object> fields = result.get(entry.getKey());
            if (fields == null) {
                fields = new HashMap<>();
                result.put(entry.getKey(), fields);
            }
            fields.put(InstanceConstants.FIELD_MOUNTS, entry.getValue());
        }

        return result;
    }

    @Override
    protected Long getId(Object obj) {
        if (obj instanceof Volume) {
            return ((Volume) obj).getId();
        }
        return null;
    }

}
