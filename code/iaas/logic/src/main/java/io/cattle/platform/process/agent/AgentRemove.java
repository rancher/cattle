package io.cattle.platform.process.agent;

import io.cattle.platform.agent.util.AgentUtils;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class AgentRemove extends AbstractObjectProcessHandler {

    private static final Logger log = LoggerFactory.getLogger(AgentRemove.class);

    @Inject
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
                deactivateThenScheduleRemove(obj, state.getData());
            }
        }

        deactivateThenScheduleRemove(objectManager.loadResource(Account.class, agent.getAccountId()), state.getData());

        return null;
    }
}
