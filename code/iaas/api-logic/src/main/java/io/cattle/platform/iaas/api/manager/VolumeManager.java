package io.cattle.platform.iaas.api.manager;

import io.cattle.platform.api.resource.jooq.AbstractJooqResourceManager;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

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
    protected Object deleteInternal(String type, String id, Object obj, ApiRequest request) {
        if (!(obj instanceof Volume)) {
            return super.deleteInternal(type, id, obj, request);
        }

        Volume volume = (Volume) obj;

        if (CommonStatesConstants.ACTIVE.equals(volume.getState())) {
            scheduleProcess(StandardProcess.DEACTIVATE, obj, CollectionUtils.asMap(VolumeConstants.REMOVE_OPTION, true));
            return getObjectManager().reload(obj);
        } else {
            return super.deleteInternal(type, id, obj, request);
        }
    }

}
