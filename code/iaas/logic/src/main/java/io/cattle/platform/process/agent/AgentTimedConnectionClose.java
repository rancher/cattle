package io.cattle.platform.process.agent;

import javax.inject.Named;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.iaas.event.IaasEvents;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AgentBasedProcessHandler;
import io.cattle.platform.util.type.Priority;

import com.netflix.config.DynamicLongProperty;

@Named
public class AgentTimedConnectionClose extends AgentBasedProcessHandler {

    private static final DynamicLongProperty TIMEOUT = ArchaiusUtil.getLong("agent.reconnect.disconnect.every.seconds");

    public AgentTimedConnectionClose() {
        setShouldContinue(true);
        setCommandName(IaasEvents.AGENT_CLOSE);
        setProcessNames(new String[] { "agent.reconnect" });
        setPriority(Priority.PRE);
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        DataAccessor prop = DataAccessor
                .fromMap(state.getData())
                .withScope(AgentTimedConnectionClose.class)
                .withKey("lastDisconnect");

        Long lastDisconnect = prop.as(Long.class);

        if ( lastDisconnect == null || System.currentTimeMillis() - lastDisconnect > (TIMEOUT.get() * 1000) ) {
            prop.set(System.currentTimeMillis());
            super.handle(state, process);
        }

        return null;
   }

}
