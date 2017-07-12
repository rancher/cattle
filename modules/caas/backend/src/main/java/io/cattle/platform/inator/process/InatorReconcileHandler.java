package io.cattle.platform.inator.process;

import com.google.common.util.concurrent.ListenableFuture;
import io.cattle.platform.async.utils.AsyncUtils;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.CompletableLogic;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.engine.manager.LoopFactory;
import io.cattle.platform.engine.manager.LoopManager;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.ObjectUtils;

public class InatorReconcileHandler implements ProcessHandler, CompletableLogic {

    ObjectManager objectManager;
    LoopManager loopManager;

    public InatorReconcileHandler(ObjectManager objectManager, LoopManager loopManager) {
        super();
        this.objectManager = objectManager;
        this.loopManager = loopManager;
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        String resourceState = ObjectUtils.getState(state.getResource());
        if (CommonStatesConstants.REGISTERING.equals(resourceState) || CommonStatesConstants.ERRORING.equals(resourceState)) {
            return null;
        }

        Object resource = state.getResource();
        Long id = new Long(state.getResourceId());
        String type = objectManager.getType(resource);

        ListenableFuture<?> future = null;
        if (resource instanceof Service) {
            future = loopManager.kick(LoopFactory.RECONCILE, type, id, resource);
        } else if (resource instanceof DeploymentUnit) {
            future = loopManager.kick(LoopFactory.DU_RECONCILE, type, id, resource);
        }

        return new HandlerResult().withFuture(future);
    }

    @Override
    public HandlerResult complete(ListenableFuture<?> future, ProcessState state, ProcessInstance process) {
        AsyncUtils.get(future);
        return null;
    }

}