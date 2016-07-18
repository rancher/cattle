package io.cattle.platform.process.common.handler;

import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.util.type.InitializationTask;
import io.cattle.platform.util.type.Priority;

public class AgentBasedProcessHandler extends AgentBasedProcessLogic implements InitializationTask, Priority, ProcessHandler {
}