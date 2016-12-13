package io.cattle.platform.process.image;

import io.cattle.platform.core.model.ImageStoragePoolMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import javax.inject.Named;

@Named
public class ImageRemove extends AbstractDefaultProcessHandler {

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        for (ImageStoragePoolMap map : objectManager.children(state.getResource(), ImageStoragePoolMap.class)) {
            deactivateThenRemove(map, state.getData());
        }
        return null;
    }

}