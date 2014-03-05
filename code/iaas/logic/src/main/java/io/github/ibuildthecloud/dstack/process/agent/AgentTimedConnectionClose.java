package io.github.ibuildthecloud.dstack.process.agent;

import javax.inject.Named;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.iaas.event.IaasEvents;
import io.github.ibuildthecloud.dstack.object.util.DataAccessor;
import io.github.ibuildthecloud.dstack.process.common.handler.AgentBasedProcessHandler;
import io.github.ibuildthecloud.dstack.util.type.Priority;

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
            return super.handle(state, process);
        } else {
            return null;
        }
   }

}
