package io.cattle.platform.sample.data;

import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.ServiceIndexTable.*;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.ServiceIndex;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SampleDataStartupV8 extends AbstractSampleData {

    @Override
    protected String getName() {
        return "sampleDataVersion8";
    }

    @Override
    protected void populatedData(Account system, List<Object> toCreate) {
        List<Instance> instances = objectManager
                .find(Instance.class, INSTANCE.REMOVED, new Condition(ConditionType.NULL));
        List<ServiceIndex> serviceIndexes = objectManager
                .find(ServiceIndex.class, SERVICE_INDEX.REMOVED, new Condition(ConditionType.NULL));
        Map<Long, ServiceIndex> serviceIndexIdsToIndexes = new HashMap<>();
        for (ServiceIndex index : serviceIndexes) {
            serviceIndexIdsToIndexes.put(index.getId(), index);
        }
        for (Instance instance : instances) {
            Long indexId = instance.getServiceIndexId();
            if (indexId == null) {
                continue;
            }
            ServiceIndex index = serviceIndexIdsToIndexes.get(indexId);
            if (index == null) {
                continue;
            }
            Map<String, Object> data = new HashMap<>();
            data.put(InstanceConstants.FIELD_SERVICE_INSTANCE_SERVICE_INDEX, index.getServiceIndex());
            objectManager.setFields(instance, data);
        }
    }
}
