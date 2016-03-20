package io.cattle.platform.sample.data;

import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.ServiceIndexTable.*;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.ServiceIndex;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SampleDataStartupV5 extends AbstractSampleData {

    @Override
    protected String getName() {
        return "sampleDataVersion5";
    }

    @Override
    protected void populatedData(Account system, List<Object> toCreate) {
        List<Instance> instances = objectManager
                .find(Instance.class, INSTANCE.REMOVED, new Condition(ConditionType.NULL));
        List<ServiceIndex> serviceIndexes = objectManager
                .find(ServiceIndex.class, SERVICE_INDEX.REMOVED, new Condition(ConditionType.NULL));
        List<Long> serviceIndexIds = new ArrayList<>();
        for (ServiceIndex index : serviceIndexes) {
            serviceIndexIds.add(index.getId());
        }
        for (Instance instance : instances) {
            Long toUpdate = DataAccessor.fieldLong(instance, InstanceConstants.FIELD_SERVICE_INSTANCE_SERVICE_INDEX_ID);
            if (toUpdate != null && serviceIndexIds.contains(toUpdate)) {
                Map<String, Object> data = new HashMap<>();
                data.put(InstanceConstants.FIELD_SERVICE_INSTANCE_SERVICE_INDEX_ID, toUpdate);
                objectManager.setFields(instance, data);
            }
        }
    }
}
