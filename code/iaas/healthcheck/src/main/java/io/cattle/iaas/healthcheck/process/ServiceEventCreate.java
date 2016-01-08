package io.cattle.iaas.healthcheck.process;

import io.cattle.iaas.healthcheck.service.HealthcheckService;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.model.ServiceEvent;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.util.type.Priority;

import javax.inject.Inject;

public class ServiceEventCreate extends AbstractObjectProcessHandler implements ProcessHandler, Priority {

    @Inject
    HealthcheckService healthcheckService;

    @Override
    public String[] getProcessNames() {
        return new String[] { "serviceevent.create" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        ServiceEvent event = (ServiceEvent)state.getResource();
        if (event.getInstanceId() == null) {
            return null;
        }

        healthcheckService.updateHealthcheck(event.getHealthcheckUuid().split("_")[0], event.getExternalTimestamp(),
                getHealthState(event.getReportedHealth()));

        return null;
    }

    protected String getHealthState(String reportedHealth) {
        String healthState = "";
        if (reportedHealth.equals("UP")) {
            healthState = HealthcheckConstants.HEALTH_STATE_HEALTHY;
        } else if (reportedHealth.equals("INIT")) {
            healthState = HealthcheckConstants.HEALTH_STATE_INITIALIZING;
        } else {
            healthState = HealthcheckConstants.HEALTH_STATE_UNHEALTHY;
        }
        return healthState;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }
}
