package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.activity.ActivityService;
import io.cattle.platform.core.addon.InServiceUpgradeStrategy;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.servicediscovery.api.service.ServiceDataManager;
import io.cattle.platform.servicediscovery.service.DeploymentManager;
import io.cattle.platform.servicediscovery.upgrade.UpgradeManager;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ServiceRollback extends AbstractDefaultProcessHandler {

    @Inject
    JsonMapper jsonMapper;
    @Inject
    UpgradeManager upgradeManager;
    @Inject
    ActivityService activityService;
    @Inject
    ServiceDataManager serviceDataMgr;
    @Inject
    DeploymentManager deploymentMgr;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        final Service service = (Service) state.getResource();
        InServiceUpgradeStrategy strategy = serviceDataMgr.getUpgradeStrategyFromServiceRevision(service);
        if (strategy == null) {
            return null;
        }

        if (strategy != null) {
            activityService.run(service, "service.rollback", "Rolling back service", new Runnable() {
                @Override
                public void run() {
                    upgradeManager.rollback(service, strategy);
                    if (DataAccessor.fieldBool(service, ServiceConstants.FIELD_FINISH_UPGRADE)) {
                        // v1 rollback
                        upgradeManager.finishUpgrade(service, true);
                    } else {
                        // v2 rollback, no cleanup
                        deploymentMgr.activate(service);
                    }
                }
            });
        } 
        return null;
    }
}