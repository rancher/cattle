package io.cattle.iaas.healthcheck.process;

import static io.cattle.platform.core.model.tables.InstanceTable.*;
import io.cattle.iaas.healthcheck.service.HealthcheckService;
import io.cattle.iaas.healthcheck.service.HealthcheckService.HealthcheckInstanceType;
import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class InstanceHealthcheckRegister extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {
    @Inject
    JsonMapper jsonMapper;

    @Inject
    HealthcheckService healtcheckService;

    @Inject
    ObjectManager objectManager;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance) state.getResource();

        InstanceHealthCheck healthCheck = DataAccessor.field(instance,
                InstanceConstants.FIELD_HEALTH_CHECK, jsonMapper, InstanceHealthCheck.class);

        // set healthcheck
        if (healthCheck != null) {
            if (instance.getHealthState() == null) {
                objectManager.setFields(instance, INSTANCE.HEALTH_STATE,
                        HealthcheckConstants.HEALTH_STATE_INITIALIZING, INSTANCE.HEALTH_UPDATED, new Date());
            } else if (instance.getHealthState().equalsIgnoreCase(HealthcheckConstants.HEALTH_STATE_HEALTHY)) {
                healtcheckService.updateInstanceHealthState(instance, HealthcheckConstants.HEALTH_STATE_REINITIALIZING);
            }
            healtcheckService.registerForHealtcheck(HealthcheckInstanceType.INSTANCE, instance.getId());
        }
        return null;
    }

    @Override
    public String[] getProcessNames() {
        return new String[] { "instance.start" };
    }

    @Override
    public int getPriority() {
        return Priority.PRE;
    }

}
