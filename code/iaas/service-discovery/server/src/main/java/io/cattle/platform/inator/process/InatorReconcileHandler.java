package io.cattle.platform.inator.process;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.CompletableLogic;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.manager.LoopManager;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.loop.LoopFactoryImpl;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.util.type.Priority;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.util.concurrent.ListenableFuture;

@Named
public class InatorReconcileHandler extends AbstractObjectProcessHandler implements Priority, CompletableLogic {

    @Inject
    ObjectManager objectManager;
    @Inject
    LoopManager loopManager;

    @Override
    public String[] getProcessNames() {
        return new String[] {"service.*", "deploymentunit.*"};
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
            future = loopManager.kick(LoopFactoryImpl.RECONCILE, type, id, resource);
        } else if (resource instanceof DeploymentUnit) {
            future = loopManager.kick(LoopFactoryImpl.DU_RECONCILE, type, id, resource);
        }

        return new HandlerResult().withFuture(future);
    }

    @Override
    public HandlerResult complete(ListenableFuture<?> future, ProcessState state, ProcessInstance process) {
        return null;
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }

}