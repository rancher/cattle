package io.cattle.platform.inator.process;

import io.cattle.platform.async.utils.AsyncUtils;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.manager.LoopManager;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.loop.LoopFactoryImpl;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.util.type.Priority;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.util.concurrent.ListenableFuture;

@Named
public class InatorReconcileHandler extends AbstractObjectProcessHandler implements Priority {

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

        if (!isIgnore(state)) {
            Object resource = state.getResource();
            Long id = new Long(state.getResourceId());
            String type = objectManager.getType(resource);

            ListenableFuture<?> future = null;
            if (resource instanceof Service) {
                future = loopManager.kick(LoopFactoryImpl.RECONCILE, type, id, resource);
            } else if (resource instanceof DeploymentUnit) {
                future = loopManager.kick(LoopFactoryImpl.DU_RECONCILE, type, id, resource);
            }

            AsyncUtils.get(future, 3, TimeUnit.SECONDS);
        }

        objectManager.reload(state.getResource());
        return null;
    }

    private boolean isIgnore(ProcessState state) {
        return DataAccessor
            .fromMap(state.getData())
            .withScope(InatorReconcileHandler.class)
            .withKey("ignore")
            .withDefault(false)
            .as(Boolean.class);
    }

    public static Map<String, Object> setIgnore(Map<String, Object> data) {
        DataAccessor
            .fromMap(data)
            .withScope(InatorReconcileHandler.class)
            .withKey("ignore")
            .set(true);
        return data;
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }

}