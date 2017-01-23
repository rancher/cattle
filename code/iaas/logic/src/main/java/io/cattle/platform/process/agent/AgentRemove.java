package io.cattle.platform.process.agent;

import io.cattle.platform.agent.util.AgentUtils;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.StoragePoolConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class AgentRemove extends AbstractObjectProcessHandler {

    private static final Logger log = LoggerFactory.getLogger(AgentRemove.class);

    @Inject
    @Named("CoreSchemaFactory")
    SchemaFactory schemaFactory;

    @Override
    public String[] getProcessNames() {
        return new String[] {"agent.remove"};
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Agent agent = (Agent)state.getResource();

        for (String type : AgentUtils.AGENT_RESOURCES.get()) {
            Class<?> clz = schemaFactory.getSchemaClass(type);
            if (clz == null) {
                log.error("Failed to find class for [{}]", type);
                continue;
            }

            
            for (Object obj : objectManager.children(agent, clz)) {
                if (obj instanceof StoragePool) {
                    StoragePool sp = (StoragePool)obj;
                    if (StoragePoolConstants.TYPE.equals(sp.getKind())) {
                        // Don't automatically delete shared storage pools
                        continue;
                    }
                }
                deactivateThenScheduleRemove(obj, state.getData());
            }
        }

        deactivateThenScheduleRemove(objectManager.loadResource(Account.class, agent.getAccountId()), state.getData());

        List<Long> authedRoleAccountIds = DataAccessor.fieldLongList(agent, AgentConstants.FIELD_AUTHORIZED_ROLE_ACCOUNTS);
        for (Long accountId : authedRoleAccountIds) {
            deactivateThenScheduleRemove(objectManager.loadResource(Account.class, accountId), state.getData());
        }

        return null;
    }
}
