package io.cattle.platform.process.agent;

import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;

import javax.inject.Named;

@Named
public class AgentHostStatePostUpdate extends AgentHostStateUpdate implements ProcessPostListener {

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        return postHandle(state, process);
    }

}