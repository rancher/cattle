package io.cattle.iaas.healthcheck.process;

import static io.cattle.platform.core.model.tables.InstanceTable.*;

import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import java.util.Date;

import javax.inject.Named;

@Named
public class InstanceHealthCheckUpdate extends AbstractObjectProcessLogic implements ProcessPostListener {

    @Override
    public String[] getProcessNames() {
        return new String[] { HealthcheckConstants.PROCESS_UPDATE_HEALTHY,
                HealthcheckConstants.PROCESS_UPDATE_UNHEALTHY, HealthcheckConstants.PROCESS_UPDATE_REINITIALIZING };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Object now = state.getData().get("now");
        Long nowLong = System.currentTimeMillis();
        if (now instanceof Number) {
            nowLong = ((Number) now).longValue();
        } else {
            state.getData().put("now", nowLong);
        }
        return new HandlerResult(INSTANCE.HEALTH_UPDATED, new Date(nowLong));
    }
}

