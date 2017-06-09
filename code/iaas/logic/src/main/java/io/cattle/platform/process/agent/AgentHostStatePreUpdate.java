package io.cattle.platform.process.agent;

import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;

import javax.inject.Named;

@Named
public class AgentHostStatePreUpdate extends AgentHostStateUpdate implements ProcessPreListener {

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        return preHandle(state, process);
    }

}