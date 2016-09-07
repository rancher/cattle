package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.activity.ActivityService;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.upgrade.UpgradeManager;

import javax.inject.Inject;

public class ServiceFinishUpgrade extends AbstractDefaultProcessHandler {

    @Inject
    UpgradeManager upgradeManager;
    @Inject
    ActivityService activityService;

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceDiscoveryConstants.PROCESS_SERVICE_FINISH_UPGRADE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        final Service service = (Service) state.getResource();
        activityService.run(service, "service.finishupgrade", "Finishing upgrade", new Runnable() {
            @Override
            public void run() {
                upgradeManager.finishUpgrade(service, true);
            }
        });

        return null;
    }
}

