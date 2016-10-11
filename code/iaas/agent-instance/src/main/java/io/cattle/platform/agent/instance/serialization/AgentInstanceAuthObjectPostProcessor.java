package io.cattle.platform.agent.instance.serialization;

import io.cattle.platform.agent.util.AgentUtils;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.serialization.ObjectTypeSerializerPostProcessor;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class AgentInstanceAuthObjectPostProcessor implements ObjectTypeSerializerPostProcessor {

    ObjectManager objectManager;

    @Override
    public String[] getTypes() {
        return new String[] { InstanceConstants.TYPE };
    }

    @Override
    public void process(Object obj, String type, Map<String, Object> data) {
        if (!(obj instanceof Instance)) {
            return;
        }

        Instance instance = (Instance) obj;

        Agent agent = objectManager.loadResource(Agent.class, instance.getAgentId());
        if (agent == null) {
            return;
        }

        List<Long> authedRoleAccountIds = DataAccessor.fieldLongList(agent, AgentConstants.FIELD_AUTHORIZED_ROLE_ACCOUNTS);
        if (authedRoleAccountIds.isEmpty()) {
            Map<String, Object> auth = AgentUtils.getAgentAuth(agent, objectManager);
            setAuthEnvVars(data, auth);
        } else {
            // Primary agent account
            Account account = objectManager.loadResource(Account.class, agent.getAccountId());
            Map<String, Object> auth = AgentUtils.getAccountScopedAuth(account, objectManager, account.getKind());
            setAuthEnvVars(data, auth);

            // Secondary authed roles
            for (Long accountId : authedRoleAccountIds) {
                account = objectManager.loadResource(Account.class, accountId);
                String scope = null;
                if (DataAccessor.fromDataFieldOf(account).withKey(AccountConstants.DATA_ACT_AS_RESOURCE_ACCOUNT).withDefault(false).as(Boolean.class)) {
                    scope = "ENVIRONMENT";
                } else if (DataAccessor.fromDataFieldOf(account).withKey(AccountConstants.DATA_ACT_AS_RESOURCE_ADMIN_ACCOUNT).withDefault(false)
                        .as(Boolean.class)) {
                    scope = "ENVIRONMENT_ADMIN";
                }
                if (scope != null) {
                    Map<String, Object> secondaryAuth = AgentUtils.getAccountScopedAuth(account, objectManager, scope);
                    setAuthEnvVars(data, secondaryAuth);
                }
            }
        }
    }

    void setAuthEnvVars(Map<String, Object> data, Map<String, Object> auth) {
        if (auth != null) {
            Map<String, Object> fields = DataUtils.getWritableFields(data);
            for (Map.Entry<String, Object> entry : auth.entrySet()) {
                DataAccessor.fromMap(fields).withScopeKey(InstanceConstants.FIELD_ENVIRONMENT).withKey(entry.getKey()).set(entry.getValue());
            }
        }
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

}
