package io.cattle.platform.iaas.api.manager;

import io.cattle.platform.api.resource.jooq.AbstractJooqResourceManager;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.StoragePoolDao;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeStoragePoolMap;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.ObjectUtils;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;

public class VolumeManager extends AbstractJooqResourceManager {

    @Inject
    StoragePoolDao storagePoolDao;
    @Inject
    EventService eventService;

    @Override
    public String[] getTypes() {
        return new String[] { "volume" };
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Volume.class };
    }

    @Override
    protected <T> T createAndScheduleObject(Class<T> clz, Map<String, Object> properties) {
        String driver = (String)properties.get(VolumeConstants.FIELD_VOLUME_DRIVER);
        String name = (String)properties.get(ObjectMetaDataManager.NAME_FIELD);
        if (StringUtils.isNotEmpty(driver) && StringUtils.isNotEmpty(name)) {
            String uri = String.format("%s:///%s", driver, name);
            properties.put(VolumeConstants.FIELD_URI, uri);
            properties.put(VolumeConstants.FIELD_DEVICE_NUM, -1);
        }

        T v = super.createAndScheduleObject(clz, properties);

        if (!VolumeConstants.LOCAL_DRIVER.equals(driver)) {
            Object aId = ObjectUtils.getAccountId(v);
            if (aId != null) {
                List<? extends StoragePool> pools = storagePoolDao.findStoragePoolByDriverName((Long)aId, driver);
                if (pools.size() > 0) {
                    StoragePool sp = pools.get(0);
                    VolumeStoragePoolMap vspm = getObjectManager().newRecord(VolumeStoragePoolMap.class);
                    vspm.setStoragePoolId(sp.getId());
                    Long volumeId = (Long)ObjectUtils.getId(v);
                    vspm.setVolumeId(volumeId);
                    getObjectManager().create(vspm);
                    ObjectUtils.publishChanged(eventService, getObjectManager(), sp);
                }
            }
        }

        return v;
    }
}
