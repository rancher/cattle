package io.cattle.platform.process.generic;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import java.util.Map;

import javax.inject.Named;

@Named
public class ActivateByDefault extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {

        String type = getObjectManager().getType(state.getResource());
        if (ArchaiusUtil.getBoolean("activate.by.default." + type).get() ||
                DataAccessor.fieldBool(state.getResource(), "activateOnCreate")) {
            String chain = objectProcessManager.getStandardProcessName(StandardProcess.ACTIVATE, state.getResource());
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

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

    @Override
    public String[] getProcessNames() {
        return new String[] { "*.create" };
    }

}
