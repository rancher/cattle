package io.cattle.iaas.healthcheck.process;

import static io.cattle.platform.core.model.tables.InstanceTable.INSTANCE;
import io.cattle.iaas.healthcheck.service.HealthcheckService;
import io.cattle.iaas.healthcheck.service.HealthcheckService.HealthcheckInstanceType;
import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class InstanceHealthcheckRegister extends AbstractObjectProcessLogic implements ProcessPreListener, Priority {
    @Inject
    JsonMapper jsonMapper;

    @Inject
    HealthcheckService healtcheckService;

    @Inject
    ObjectManager objectManager;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Nic nic = (Nic) state.getResource();
        Instance instance = objectManager.loadResource(Instance.class, nic.getInstanceId());

        if (instance == null) {
            return null;
        }

        InstanceHealthCheck healthCheck = DataAccessor.field(instance,
                InstanceConstants.FIELD_HEALTH_CHECK, jsonMapper, InstanceHealthCheck.class);

        // set healthcheck
        if (healthCheck != null) {
            Long startCount = instance.getStartCount() == null ? 0 : instance.getStartCount() + 1;
            objectManager.setFields(instance, INSTANCE.START_COUNT, startCount, INSTANCE.HEALTH_STATE,
                    HealthcheckConstants.HEALTH_STATE_INITIALIZING);
            healtcheckService.registerForHealtcheck(HealthcheckInstanceType.INSTANCE, instance.getId());
        }
        return null;
    }

    @Override
    public String[] getProcessNames() {
        return new String[] { "nic.activate" };
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
