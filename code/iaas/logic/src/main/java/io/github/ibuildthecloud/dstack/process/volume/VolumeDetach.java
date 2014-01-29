package io.github.ibuildthecloud.dstack.process.volume;

import static io.github.ibuildthecloud.dstack.core.model.tables.VolumeTable.*;

import javax.inject.Named;

import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.process.base.AbstractDefaultProcessHandler;

@Named
public class VolumeDetach extends AbstractDefaultProcessHandler {

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        return new HandlerResult(
                VOLUME.DEVICE_NUMBER, null,
                VOLUME.INSTANCE_ID, null);
    }

}
