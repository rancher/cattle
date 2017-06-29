package io.cattle.platform.sample.data;

import static io.cattle.platform.core.model.tables.StackTable.*;

import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SampleDataStartupV11 extends AbstractSampleData {

    public SampleDataStartupV11(ObjectManager objectManager, ObjectProcessManager processManager, AccountDao accountDao, JsonMapper jsonMapper,
            LockManager lockManager) {
        super(objectManager, processManager, accountDao, jsonMapper, lockManager);
    }

    @Override
    protected String getName() {
        return "sampleDataVersion11";
    }

    @Override
    protected void populatedData(Account system, List<Object> toCreate) {
        List<Stack> stacks = objectManager
                .find(Stack.class, STACK.REMOVED, new Condition(ConditionType.NULL),
                        STACK.EXTERNAL_ID, "system://kubernetes");
        for (Stack stack : stacks) {
            Map<String, Object> data = new HashMap<>();
            data.put("externalId", "system-catalog://library:kubernetes:0");
            objectManager.setFields(stack, data);
        }
    }
}