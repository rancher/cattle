package io.cattle.platform.process.externalevent;

import static io.cattle.platform.core.model.tables.AgentTable.*;

import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.ExternalEvent;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

@Named
public class ExternalEventPreCreate extends AbstractObjectProcessLogic implements ProcessPreListener, Priority {

    @Override
    public String[] getProcessNames() {
        return new String[] { "externalevent.create" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        // event's account id is set to the agent that submitted. This will change it to the actual user's account id.
        ExternalEvent event = (ExternalEvent)state.getResource();

        List<Agent> agents = objectManager.find(Agent.class, AGENT.ACCOUNT_ID, event.getAccountId());
        if (agents.size() == 1) {
            Agent agent = agents.get(0);
            Long resourceAccId = DataAccessor.fromDataFieldOf(agent).withKey(AgentConstants.DATA_AGENT_RESOURCES_ACCOUNT_ID).as(Long.class);
            Map<String, Object> data = new HashMap<String, Object>();
            if (resourceAccId != null) {
                data.put(ObjectMetaDataManager.ACCOUNT_FIELD, resourceAccId);
            }
            if (event.getReportedAccountId() != null) {
                data.put(ExternalEventConstants.FIELD_REPORTED_ACCOUNT_ID, event.getReportedAccountId());
            } else {
                data.put(ExternalEventConstants.FIELD_REPORTED_ACCOUNT_ID, event.getAccountId());
            }

            return new HandlerResult(data);
        }

        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }
}
