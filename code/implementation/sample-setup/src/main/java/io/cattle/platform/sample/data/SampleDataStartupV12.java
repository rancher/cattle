package io.cattle.platform.sample.data;

import static io.cattle.platform.core.model.tables.ConfigItemTable.*;
import static io.cattle.platform.core.model.tables.StackTable.*;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.ConfigItem;
import io.cattle.platform.core.model.Stack;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SampleDataStartupV12 extends AbstractSampleData {
    private static final String CONFIG_NAME = "stack-reconcile";

    @Override
    protected String getName() {
        return "sampleDataVersion12";
    }

    @Override
    protected void populatedData(Account system, List<Object> toCreate) {
        List<Stack> stacks = objectManager
                .find(Stack.class, STACK.REMOVED, new Condition(ConditionType.NULL));
        updateConfigItem(stacks);
    }

    protected void updateConfigItem(List<Stack> stacks) {
        ConfigItem ci = objectManager.findAny(ConfigItem.class,
                    CONFIG_ITEM.NAME,
                    CONFIG_NAME);
        Map<String, Object> props = new HashMap<>();
        props.put("sourceVersion", 1);
        objectManager.setFields(ci, props);
    }
}
