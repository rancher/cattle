package io.cattle.platform.sample.data;

import static io.cattle.platform.core.model.tables.InstanceTable.*;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;

public class SampleDataStartupV9 extends AbstractSampleData {

    @Override
    protected String getName() {
        return "sampleDataVersion9";
    }

    @Override
    protected void populatedData(Account system, List<Object> toCreate) {
        List<Instance> instances = objectManager
                .find(Instance.class, INSTANCE.REMOVED, new Condition(ConditionType.NULL));

        for (Instance instance : instances) {
            Map<String, Object> data = new HashMap<>();
            List<String> dnsSearch = DataAccessor.fieldStringList(instance,
                    "dnsSearch");
            if (!dnsSearch.isEmpty()) {
                data.put(InstanceConstants.FIELD_DNS_SEARCH_INTERNAL, Joiner.on(",").join(dnsSearch));
            }

            List<String> dns = DataAccessor.fieldStringList(instance,
                    "dns");
            if (!dns.isEmpty()) {
                data.put(InstanceConstants.FIELD_DNS_INTERNAL, Joiner.on(",").join(dns));
            }

            if (!data.isEmpty()) {
                objectManager.setFields(instance, data);
            }
        }
    }
}
