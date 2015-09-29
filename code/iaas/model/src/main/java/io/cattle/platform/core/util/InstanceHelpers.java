package io.cattle.platform.core.util;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InstanceHelpers {

    public static List<Volume> extractVolumesFromMounts(Instance instance, ObjectManager objectManager) {
        List<Volume> volumes = new ArrayList<Volume>();

        Map<String, Object> dataVolumeMounts = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_DATA_VOLUME_MOUNTS);

        if (dataVolumeMounts != null) {
            for (Map.Entry<String, Object> entry : dataVolumeMounts.entrySet()) {
                Volume v = objectManager.loadResource(Volume.class, ((Number) entry.getValue()).longValue());
                if (v != null) {
                    volumes.add(v);
                }
            }
        }
        return volumes;
    }

}
