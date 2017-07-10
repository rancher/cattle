package io.cattle.platform.api.volume;

import io.cattle.platform.api.resource.DefaultResourceManager;
import io.cattle.platform.api.resource.DefaultResourceManagerSupport;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.StoragePoolDao;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.ObjectUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.List;

public class VolumeManager extends DefaultResourceManager {

    StoragePoolDao storagePoolDao;
    EventService eventService;

    public VolumeManager(DefaultResourceManagerSupport support) {
        super(support);
    }

    @Override
    public Object create(String type, ApiRequest request) {
        Object v = super.create(type, request);

        String driver = DataAccessor.fieldString(v, VolumeConstants.FIELD_VOLUME_DRIVER);
        if (v instanceof Volume && !VolumeConstants.LOCAL_DRIVER.equals(driver)) {
            Object aId = ObjectUtils.getAccountId(v);
            if (aId != null) {
                List<? extends StoragePool> pools = storagePoolDao.findStoragePoolByDriverName((Long)aId, driver);
                if (pools.size() == 1) {
                    StoragePool sp = pools.get(0);
                    ((Volume) v).setStoragePoolId(pools.get(0).getId());
                    ObjectUtils.publishChanged(eventService, objectResourceManagerSupport.getObjectManager(), sp);
                }
            }
        }

        return v;
    }
}
