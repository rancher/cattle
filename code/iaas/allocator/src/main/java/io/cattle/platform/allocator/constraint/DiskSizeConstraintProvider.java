package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationLog;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class DiskSizeConstraintProvider implements AllocationConstraintsProvider {

    @Inject
    ObjectManager objectManager;

    @Override
    public void appendConstraints(AllocationAttempt attempt, AllocationLog log, List<Constraint> constraints) {
        Instance instance = attempt.getInstance();
        if (instance == null) {
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, String> labels = DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_LABELS)
                .as(Map.class);
        if (labels == null) {
            return;
        }

        for (Map.Entry<String, String> labelEntry : labels.entrySet()) {
            String labelKey = labelEntry.getKey();
            String labelPrefix = SystemLabels.LABEL_SCHEDULER_DISKSIZE_PREFIX;
            if (labelKey.startsWith(labelPrefix) && labelKey.length() > labelPrefix.length()) {
                constraints.add(new DiskSizeConstraint(instance, objectManager));
                return;
            }
        }
    }

    @Override
    public boolean isCritical() {
        return true;
    }

}
