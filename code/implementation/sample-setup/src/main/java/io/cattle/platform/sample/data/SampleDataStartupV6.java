package io.cattle.platform.sample.data;

import static io.cattle.platform.core.model.tables.ConfigItemStatusTable.*;
import static io.cattle.platform.core.model.tables.ConfigItemTable.*;
import static io.cattle.platform.core.model.tables.StackTable.*;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.ConfigItem;
import io.cattle.platform.core.model.ConfigItemStatus;
import io.cattle.platform.core.model.Stack;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SampleDataStartupV6 extends AbstractSampleData {
    private static final String CONFIG_NAME = "stack-reconcile";
    @Override
    protected String getName() {
        return "sampleDataVersion6";
    }

    @Override
    protected void populatedData(Account system, List<Object> toCreate) {
        List<Stack> stacks = objectManager
                .find(Stack.class, STACK.REMOVED, new Condition(ConditionType.NULL));
        createConfigItem();
        createConfigItemStatus(stacks);
    }

    protected void createConfigItemStatus(List<Stack> stacks) {
        for (Stack stack : stacks) {
            ConfigItemStatus existing = objectManager.findAny(ConfigItemStatus.class, CONFIG_ITEM_STATUS.NAME,
                    CONFIG_NAME, CONFIG_ITEM_STATUS.RESOURCE_ID, stack.getId(), CONFIG_ITEM_STATUS.STACK_ID,
                    stack.getId(), CONFIG_ITEM_STATUS.RESOURCE_TYPE, "environment_id");
            if (existing == null) {
                try {
                    Map<String, Object> props = new HashMap<>();
                    props.put("name", CONFIG_NAME);
                    props.put("requestedVersion", 1);
                    props.put("appliedVersion", 0);
                    props.put("sourceVersion", "");
                    props.put("resourceId", stack.getId());
                    props.put("environmentId", stack.getId());
                    props.put("resourceType", "environment_id");
                    objectManager.create(ConfigItemStatus.class, props);
                } catch (Exception ex) {
                }
            }
        }
    }

    protected void createConfigItem() {
        ConfigItem item = objectManager.findAny(ConfigItem.class, CONFIG_ITEM.NAME,
                CONFIG_NAME);
        if (item == null) {
            Map<String, Object> props = new HashMap<>();
            props.put("name", CONFIG_NAME);
            props.put("sourceVersion", "");
            objectManager.create(ConfigItem.class, props);
        }
    }
}
