package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.activity.ActivityService;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.upgrade.UpgradeManager;

import javax.inject.Inject;

public class ServiceContinueupgrade extends AbstractDefaultProcessHandler {

    @Inject
    JsonMapper jsonMapper;

    @Inject
    UpgradeManager upgradeManager;

    @Inject
    ActivityService activityService;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        final Service service = (Service)state.getResource();
        final io.cattle.platform.core.addon.ServiceUpgrade upgrade = DataAccessor.field(service, ServiceDiscoveryConstants.FIELD_UPGRADE,
                jsonMapper, io.cattle.platform.core.addon.ServiceUpgrade.class);

        activityService.run(service, "service.continueupgrade", "Continuing service upgrade", new Runnable() {
            @Override
            public void run() {
                upgradeManager.upgrade(service, upgrade.getStrategy());
            }
        });

        return null;
    }
}
