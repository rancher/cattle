package io.cattle.platform.process.volume;

import static io.cattle.platform.core.model.tables.VolumeTable.*;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import javax.inject.Named;

@Named
public class VolumeDetach extends AbstractDefaultProcessHandler {

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        return new HandlerResult(VOLUME.DEVICE_NUMBER, null, VOLUME.INSTANCE_ID, null);
    }

}
