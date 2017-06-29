package io.cattle.platform.sample.data;

import static io.cattle.platform.core.model.tables.InstanceTable.*;

import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.List;

public class SampleDataStartupV17 extends AbstractSampleData {

    public SampleDataStartupV17(ObjectManager objectManager, ObjectProcessManager processManager, AccountDao accountDao, JsonMapper jsonMapper,
            LockManager lockManager) {
        super(objectManager, processManager, accountDao, jsonMapper, lockManager);
    }

    @Override
    protected String getName() {
        return "sampleDataVersion17";
    }

    @Override
    protected void populatedData(Account system, List<Object> toCreate) {
        List<Instance> instances = objectManager
                .find(Instance.class, INSTANCE.REMOVED, new Condition(ConditionType.NULL));
        for (Instance instance : instances) {
            Long hostId = DataAccessor.fieldLong(instance, "hostId");
            if (hostId != null) {
                instance.setHostId(hostId);
            }
            objectManager.persist(instance);
        }
    }
}
