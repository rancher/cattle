package io.cattle.iaas.healthcheck.process;

import io.cattle.platform.core.addon.HealthcheckState;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.ServiceEvent;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.engine.manager.LoopFactory;
import io.cattle.platform.engine.manager.LoopManager;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;

import java.util.List;

public class ServiceEventCreate implements ProcessHandler {

    ObjectManager objectManager;
    LoopManager loopManager;

    public ServiceEventCreate(ObjectManager objectManager, LoopManager loopManager) {
        this.objectManager = objectManager;
        this.loopManager = loopManager;
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

        Instance instance = objectManager.loadResource(Instance.class, event.getInstanceId());
        if (instance == null) {
            return null;
        }

        boolean changed = false;
        String health = getHealthState(event.getReportedHealth());
        List<HealthcheckState> hcStates = DataAccessor.fieldObjectList(instance,
                InstanceConstants.FIELD_HEALTHCHECK_STATES,
                HealthcheckState.class);
        for (HealthcheckState hcState : hcStates) {
            if (!hcState.getHostId().equals(event.getHostId())) {
                continue;
            }

            if (event.getExternalTimestamp() != null &&
                    event.getExternalTimestamp().compareTo(hcState.getExternalTimestamp()) < 0) {
                continue;
            }

            if (!hcState.getHealthState().equals(health)) {
                continue;
            }

            hcState.setExternalTimestamp(event.getExternalTimestamp());
            hcState.setHealthState(health);
            changed = true;
        }

        if (changed) {
            objectManager.setFields(instance, InstanceConstants.FIELD_HEALTHCHECK_STATES, hcStates);
            loopManager.kick(LoopFactory.HEALTHSTATE_CALCULATE, AccountConstants.TYPE, instance.getAccountId(), null);
        }

        return null;
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
}
