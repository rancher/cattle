package io.cattle.platform.process.snapshot;

import io.cattle.platform.core.constants.SnapshotConstants;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import javax.inject.Named;

@Named
public class SnapshotPreBackup extends AbstractObjectProcessLogic implements ProcessPreListener, Priority {

    @Override
    public String[] getProcessNames() {
        return new String[] { SnapshotConstants.PROCESS_BACKUP };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        state.getData().put("processId", process.getId().toString());
        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}