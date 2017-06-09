package io.cattle.platform.sample.data;

import static io.cattle.platform.core.model.tables.AgentTable.*;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;

import java.util.List;

import javax.inject.Inject;


public class SampleDataStartupV16 extends AbstractSampleData {

    public static final String DATA_AGENT_RESOURCES_ACCOUNT_ID = "agentResourcesAccountId";

    @Inject
    ObjectManager objectManager;

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