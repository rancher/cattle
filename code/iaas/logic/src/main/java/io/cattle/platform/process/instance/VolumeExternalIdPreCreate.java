package io.cattle.platform.process.instance;

import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.util.VolumeUtils;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import java.util.HashMap;
import java.util.Map;

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
        if (volume.getImageId() != null || StringUtils.isEmpty(volume.getName())) {
            return null;
        }

        Map<Object, Object> data = new HashMap<>();
        String externalId = volume.getExternalId();
        if (StringUtils.isEmpty(externalId)) {
            externalId = volume.getName();
        }
        data.put("externalId", VolumeUtils.externalId(externalId));
        return new HandlerResult(data);
    }
}
