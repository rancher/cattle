package io.cattle.platform.process.instance;

import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.util.VolumeUtils;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

@Named
public class VolumeExternalIdPreCreate extends AbstractObjectProcessLogic implements ProcessPreListener {

    @Override
    public String[] getProcessNames() {
        return new String[] { "volume.create" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Volume volume = (Volume)state.getResource();

        String externalId = volume.getExternalId();
        if (StringUtils.isEmpty(externalId)) {
            externalId = volume.getName();
        }

        return new HandlerResult("externalId", VolumeUtils.externalId(externalId));
    }
}
