package io.cattle.platform.process.generic;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;

import java.util.Map;

public class ActivateByDefault implements ProcessHandler {

    ObjectManager objectManager;
    ObjectProcessManager processManager;

    public ActivateByDefault(ObjectManager objectManager, ObjectProcessManager processManager) {
        super();
        this.objectManager = objectManager;
        this.processManager = processManager;
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        String type = objectManager.getType(state.getResource());
        if (ArchaiusUtil.getBoolean("activate.by.default." + type).get() ||
                DataAccessor.fieldBool(state.getResource(), "activateOnCreate")) {
            String chain = processManager.getStandardProcessName(StandardProcess.ACTIVATE, state.getResource());
            HandlerResult result = new HandlerResult(true, (Map<Object, Object>) null);
            result.setChainProcessName(chain);
            return result;
        } else {
            String val = ArchaiusUtil.getString("activate.by.default." + type).get();
            if (val != null && val.contains(".")) {
                HandlerResult result = new HandlerResult(true, (Map<Object, Object>) null);
                result.setChainProcessName(val);
                return result;
            }
        }

        return null;
    }

}
