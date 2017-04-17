package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessDelayException;
import io.cattle.platform.inator.Deployinator;
import io.cattle.platform.inator.Result;
import io.cattle.platform.inator.Unit.UnitState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;

import javax.inject.Inject;

public class ReconcileHandler extends AbstractObjectProcessHandler {

    @Inject
    Deployinator deployinator;

    @Override
    public String[] getProcessNames() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Object resource = state.getResource();

        Class<?> clz = null;
        if (resource instanceof Service) {
            clz = Service.class;
        } else if (resource instanceof DeploymentUnit){
            clz = DeploymentUnit.class;
        }

        if (clz == null) {
            return null;
        }

        Result result = deployinator.reconcile(clz, Long.parseLong(state.getResourceId()));
        if (result.getState() == UnitState.GOOD) {
            objectManager.reload(resource);
            return null;
        }

        throw new ProcessDelayException(null);
    }

}
