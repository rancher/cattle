package io.cattle.iaas.healthcheck.process;

import io.cattle.iaas.healthcheck.service.HealthcheckService;
import io.cattle.platform.core.model.HealthcheckInstance;
import io.cattle.platform.core.model.HealthcheckInstanceHostMap;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class HealthcheckInstanceHostMapRemovePostHandler extends AbstractObjectProcessLogic implements
        ProcessPreListener, Priority {

    @Inject
    HealthcheckService hcSvc;

    @Override
    public String[] getProcessNames() {
        return new String[] { "healthcheckinstancehostmap.remove" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        HealthcheckInstanceHostMap hostMap = (HealthcheckInstanceHostMap) state.getResource();
        HealthcheckInstance hInstance = objectManager.loadResource(HealthcheckInstance.class, hostMap.getHealthcheckInstanceId());
        if (hInstance == null || hInstance.getRemoved() != null) {
            return null;
        }
        Instance instance = objectManager.loadResource(Instance.class, hInstance.getInstanceId());
        
        if (instance == null || instance.getRemoved() != null) {
            return null;
        }
        
        hcSvc.registerForHealtcheck(HealthcheckService.HealthcheckInstanceType.INSTANCE, instance.getId());

        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
