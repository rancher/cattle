package io.cattle.platform.core.util;

import static io.cattle.platform.core.model.tables.VolumeTable.*;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InstanceHelpers {

    public static List<Volume> extractVolumesFromMounts(Instance instance, ObjectManager objectManager) {
        List<Volume> volumes = new ArrayList<Volume>();

        Map<String, Object> dataVolumeMounts = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_DATA_VOLUME_MOUNTS);

        List<Object> volumeIds = new ArrayList<Object>();
        if (dataVolumeMounts != null) {
            for (Map.Entry<String, Object> entry : dataVolumeMounts.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    volumeIds.add(((Number)entry.getValue()).longValue());
                }
            }
            Condition condition = new Condition(ConditionType.IN, volumeIds);
            volumes = objectManager.find(Volume.class, VOLUME.ID, condition);
        }
        return volumes;
    }

}
