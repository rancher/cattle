package io.cattle.platform.sample.data;

import static io.cattle.platform.core.model.tables.AgentTable.*;

import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;

import java.util.List;


public class SampleDataStartupV16 extends AbstractSampleData {

    public static final String DATA_AGENT_RESOURCES_ACCOUNT_ID = "agentResourcesAccountId";

    public SampleDataStartupV16(ObjectManager objectManager, ObjectProcessManager processManager, AccountDao accountDao, JsonMapper jsonMapper,
            LockManager lockManager) {
        super(objectManager, processManager, accountDao, jsonMapper, lockManager);
    }

    @Override
    protected String getName() {
        return "sampleDataVersion16";
    }

    @Override
    protected void populatedData(Account system, List<Object> toCreate) {
        List<Agent> agents = objectManager.find(Agent.class,
                AGENT.RESOURCE_ACCOUNT_ID, null,
                ObjectMetaDataManager.REMOVED_FIELD, null);
        for (Agent agent : agents) {
            Long id = DataAccessor.fromMap(agent.getData())
                    .withKey(DATA_AGENT_RESOURCES_ACCOUNT_ID)
                    .as(Long.class);
            if (id != null) {
                agent.setResourceAccountId(id);
                objectManager.persist(agent);
            }
        }
    }

}