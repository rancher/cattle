package io.cattle.platform.register.dao.impl;

import static io.cattle.platform.core.model.tables.AgentTable.*;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.GenericObject;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.register.dao.RegisterDao;
import io.cattle.platform.register.util.RegisterConstants;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.Map;

import javax.inject.Inject;

public class RegisterDaoImpl extends AbstractJooqDao implements RegisterDao {

    ObjectManager objectManager;

    @Override
    public Agent createAgentForRegistration(String key, GenericObject obj) {
        Map<String,Object> data = CollectionUtils.asMap(
                RegisterConstants.AGENT_DATA_REGISTRATION_KEY, key,
                AgentConstants.DATA_AGENT_RESOURCES_ACCOUNT_ID, obj.getAccountId());

        String format = DataAccessor.fieldString(obj, "agentUriFormat");
        if (format == null) {
            format = "event://%s";
        }

        Agent agent = objectManager.create(Agent.class,
                AGENT.KIND, "registeredAgent",
                AGENT.URI, String.format(format, obj.getUuid()),
                AGENT.DATA, data,
                AGENT.MANAGED_CONFIG, true);

        DataAccessor.fromDataFieldOf(obj)
                    .withKey(RegisterConstants.DATA_AGENT_ID)
                    .set(agent.getId());

        objectManager.persist(obj);

        return agent;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

}
