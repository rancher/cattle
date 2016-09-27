package io.cattle.platform.process.instance;

import io.cattle.platform.core.constants.StoragePoolConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.StoragePoolDao;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

@Named
public class VolumePreCreate extends AbstractObjectProcessLogic implements ProcessPreListener {

    @Inject
    StoragePoolDao storagePoolDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { "volume.create" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Volume v = (Volume)state.getResource();
        String driver = DataAccessor.fieldString(v, VolumeConstants.FIELD_VOLUME_DRIVER);
        if (StringUtils.isEmpty(driver) || VolumeConstants.LOCAL_DRIVER.equals(driver)) {
            return null;
        }

        List<? extends StoragePool> pools = storagePoolDao.findStoragePoolByDriverName(v.getAccountId(), driver);
        if (pools.size() == 0) {
            return null;
        }
        StoragePool sp = pools.get(0);

        Map<String, Object> data = new HashMap<>();
        List<String> volumeCap = DataAccessor.fieldStringList(sp, StoragePoolConstants.FIELD_VOLUME_CAPABILITIES);
        if (volumeCap != null && volumeCap.size() > 0) {
            data.put(ObjectMetaDataManager.CAPABILITIES_FIELD, volumeCap);
        }

        return new HandlerResult(data);
    }
}
