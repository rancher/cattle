package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.activity.ActivityService;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.servicediscovery.upgrade.UpgradeManager;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ServiceUpgrade extends AbstractDefaultProcessHandler {

    @Inject
    JsonMapper jsonMapper;
    @Inject
    UpgradeManager upgradeManager;
    @Inject
    ActivityService activityService;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        final io.cattle.platform.core.addon.ServiceUpgrade upgrade = jsonMapper.convertValue(state.getData(),
                io.cattle.platform.core.addon.ServiceUpgrade.class);
        final Service service = (Service)state.getResource();

        objectManager.setFields(service, ServiceConstants.FIELD_UPGRADE, upgrade);

        activityService.run(service, "service.upgrade", "Upgrading service", new Runnable() {
            @Override
            public void run() {
                upgradeManager.upgrade(service, upgrade.getStrategy(), ServiceConstants.STATE_UPGRADING, true, false);
            }
        });

        return null;
    }
}
