package io.cattle.platform.iaas.api.manager;

import io.cattle.platform.api.resource.jooq.AbstractJooqResourceManager;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.object.meta.ObjectMetaDataManager;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class VolumeManager extends AbstractJooqResourceManager {

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
        String driver = (String) properties.get(VolumeConstants.FIELD_VOLUME_DRIVER);
        String name = (String) properties.get(ObjectMetaDataManager.NAME_FIELD);
        if (StringUtils.isNotEmpty(driver) && StringUtils.isNotEmpty(name)) {
            String uri = String.format("%s:///%s", driver, name);
            properties.put(VolumeConstants.FIELD_URI, uri);
            properties.put(VolumeConstants.FIELD_DEVICE_NUM, -1);
        }
        return getObjectManager().create(clz, properties);
    }
}
