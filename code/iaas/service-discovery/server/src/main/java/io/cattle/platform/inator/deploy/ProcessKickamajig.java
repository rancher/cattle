package io.cattle.platform.inator.deploy;

import io.cattle.platform.engine.context.EngineContext;
import io.cattle.platform.engine.process.impl.ProcessExecutionExitException;
import io.cattle.platform.inator.Unit.UnitState;
import io.cattle.platform.inator.factory.InatorServices;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.Map;

public class ProcessKickamajig {

    private static Map<String, Object> STATE_TO_PROCESS = CollectionUtils.asMap(
        "activating", "activate",
        "deactivating", "deactivate",
        "finishing-upgrade", "finishupgrade",
        "pausing", "pause",
        "registering", "create",
        "removing", "remove",
        "restarting", "restart",
        "rolling-back", "rollback",
        "updating-inactive", "update",
        "updating-inactive", "update",
        "upgrading", "upgrade");

    InatorServices svc;

    public ProcessKickamajig(InatorServices svc) {
        this.svc = svc;
    }

    public void kickProcess(Object obj, String state, UnitState unitState) {
        if (unitState != UnitState.GOOD || EngineContext.hasParentProcess()) {
            return;
        }

        Object process = STATE_TO_PROCESS.get(state);
        if (process != null) {
            String processName = svc.processManager.getProcessName(obj, process.toString());
            if (processName != null) {
                try {
                    svc.processManager.executeProcess(processName, obj, null);
                } catch (ProcessExecutionExitException e) {
                }
            }
        }
    }

}
