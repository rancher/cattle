package io.cattle.platform.sample.data;

import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SampleDataStartupV7 extends AbstractSampleData {

    @Override
    protected String getName() {
        return "sampleDataVersion7";
    }

    @Override
    protected void populatedData(Account system, List<Object> toCreate) {
        List<Instance> instances = objectManager
                .find(Instance.class, INSTANCE.REMOVED, new Condition(ConditionType.NULL));
        Map<Long, Instance> instanceIdToInstance = new HashMap<>();
        for (Instance instance : instances) {
            instanceIdToInstance.put(instance.getId(), instance);
        }
        List<InstanceHostMap> hostMaps = objectManager
                .find(InstanceHostMap.class, INSTANCE_HOST_MAP.REMOVED, new Condition(ConditionType.NULL));

        for (InstanceHostMap hostMap : hostMaps) {
            Instance instance = instanceIdToInstance.get(hostMap.getInstanceId());
            if (instance == null) {
                continue;
            }
            Map<String, Object> data = new HashMap<>();
            data.put(InstanceConstants.FIELD_HOST_ID, hostMap.getHostId());
            objectManager.setFields(instance, data);
        }
    }
}
