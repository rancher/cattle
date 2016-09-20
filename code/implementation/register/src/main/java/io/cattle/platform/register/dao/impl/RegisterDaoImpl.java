package io.cattle.platform.register.dao.impl;

import static io.cattle.platform.core.model.tables.AgentTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.AgentConstants;
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

import org.apache.commons.lang3.StringUtils;

import com.netflix.config.DynamicStringListProperty;

public class RegisterDaoImpl extends AbstractJooqDao implements RegisterDao {

    private static DynamicStringListProperty ALLOWED_URIS = ArchaiusUtil.getList("allowed.user.agent.uri.prefix");

    ObjectManager objectManager;

    @Override
    public Agent createAgentForRegistration(String key, GenericObject obj) {
        Map<String,Object> data = CollectionUtils.asMap(
                RegisterConstants.AGENT_DATA_REGISTRATION_KEY, key,
                AgentConstants.DATA_AGENT_RESOURCES_ACCOUNT_ID, obj.getAccountId());

        String format = DataAccessor.fieldString(obj, "agentUriFormat");
        if (format != null) {
            boolean found = false;
            for (String prefix : ALLOWED_URIS.get()) {
                if (StringUtils.isNotBlank(prefix) && format.startsWith(prefix)) {
                    found = true;
                }
            }
            if (!found) {
                format = null;
            }
        }
        if (format == null) {
            format = "event://%s";
        }

        Agent agent = objectManager.create(Agent.class,
                AGENT.KIND, AccountConstants.REGISTERED_AGENT_KIND,
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
