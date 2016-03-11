package io.cattle.iaas.healthcheck.process;

import io.cattle.iaas.healthcheck.service.HealthcheckService;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.model.HealthcheckInstanceHostMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class HealthCheckReconcile extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    @Inject
    HealthcheckService hcSvc;

    @Override
    public String[] getProcessNames() {
        return new String[] { "healthcheckinstancehostmap.remove" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        HealthcheckInstanceHostMap hcihm = (HealthcheckInstanceHostMap) state.getResource();
        hcSvc.healthCheckReconcile(hcihm, HealthcheckConstants.HEALTH_STATE_RECONCILE);
        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT_OVERRIDE;
    }

}
