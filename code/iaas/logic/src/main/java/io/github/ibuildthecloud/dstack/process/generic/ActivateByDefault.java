package io.github.ibuildthecloud.dstack.process.generic;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessPostListener;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.process.common.handler.AbstractObjectProcessLogic;
import io.github.ibuildthecloud.dstack.util.type.Priority;

import java.util.Map;

import javax.inject.Named;

@Named
public class ActivateByDefault extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        HandlerResult result = new HandlerResult(true, (Map<Object,Object>)null);

        String type = getObjectManager().getType(state.getResource());
        if ( ArchaiusUtil.getBoolean("activate.by.default." + type).get() ) {
            result.shouldDelegate(true);
        }

        return result;
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
