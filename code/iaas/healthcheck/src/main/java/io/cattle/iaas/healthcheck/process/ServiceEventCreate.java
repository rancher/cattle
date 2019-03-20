package io.cattle.iaas.healthcheck.process;

import io.cattle.iaas.healthcheck.service.HealthcheckService;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.HealthcheckInstanceHostMap;
import io.cattle.platform.core.model.ServiceEvent;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.util.type.Priority;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



@Named
public class ServiceEventCreate extends AbstractObjectProcessHandler implements ProcessHandler, Priority {

    @Inject
    HealthcheckService healthcheckService;

    @Inject
    ServiceDao serviceDao;
    
    private static final Logger log = LoggerFactory.getLogger(ServiceEventCreate.class);

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



        // don't process init event as its being set by cattle on instance restart
        // that is done to avoid the scenario when init state is reported on healthcheck process restart inside the
        // agent happening around the time when instance becomes unheatlhy
        // this can lead to instance being set with reinitializing state instead of unheatlhy, and it postpones (or even
        // cancels, if reinitializing timeout is not set) instance recreation
        if ("INIT".equals(event.getReportedHealth())) {
            return null;
        }
        processHealthcheck(event);

        return null;
    }

    private void processHealthcheck(ServiceEvent event) {
        String[] splitted = event.getHealthcheckUuid().split("_");
        // find host map uuid
        HealthcheckInstanceHostMap hostMap = null;
        String uuid = null;
        if (splitted.length > 2) {
            hostMap = serviceDao.getHealthCheckInstanceUUID(splitted[0], splitted[1]);
            if (hostMap != null) {
                uuid = hostMap.getUuid();
            }
        } else {
            uuid = splitted[0];
        }
        if (uuid != null) {
            if(event.getExternalTimestamp() == null) {
                log.warn("ExternalTimestamp null for event {} hostMapUUID {}", event.getId(), uuid);
                return;
            }
            healthcheckService.updateHealthcheck(uuid, event.getExternalTimestamp(),
                    getHealthState(event.getReportedHealth()));
        }
    }

    protected String getHealthState(String reportedHealth) {
        String healthState = "";

        if (reportedHealth.equals("UP")) {
            healthState = HealthcheckConstants.HEALTH_STATE_HEALTHY;
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
