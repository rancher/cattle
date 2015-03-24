package io.cattle.platform.process.containerevent;

import static io.cattle.platform.core.model.tables.AgentTable.*;
import static io.cattle.platform.core.model.tables.ContainerEventTable.*;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.ContainerEvent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;

import org.jooq.exception.InvalidResultException;

@Named
public class ContainerEventPreCreate extends AbstractObjectProcessLogic implements ProcessPreListener, Priority {

    @Override
    public String[] getProcessNames() {
        return new String[] { "containerevent.create" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        // event's account id is set to the agent that submitted. This will
        // change it to the actual user's account id.
        // Checks to make sure agent's resource account id and the host's
        // account id match
        ContainerEvent event = (ContainerEvent)state.getResource();

        Agent agent = null;
        try {
            agent = objectManager.findOne(Agent.class, AGENT.ACCOUNT_ID, event.getAccountId());
        } catch (InvalidResultException e) {
            // Found more than one agent, don't do anything.
        }

        if ( agent == null ) {
            return null;
        }

        Long resourceAccId = DataAccessor.fromDataFieldOf(agent)
                .withKey(AgentConstants.DATA_AGENT_RESOURCES_ACCOUNT_ID).as(Long.class);

        Host host = objectManager.loadResource(Host.class, event.getHostId());

        // It should be impossible for the agent resource id and host's account
        // id to mismatch, but check just to be safe.
        if ( host.getAccountId().equals(resourceAccId) ) {
            Map<Object, Object> newFields = new HashMap<Object, Object>();
            newFields.put(CONTAINER_EVENT.ACCOUNT_ID, host.getAccountId());
            return new HandlerResult(newFields);
        }

        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }
}
