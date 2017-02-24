package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.activity.ActivityService;
import io.cattle.platform.core.addon.ServiceUpgradeStrategy;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.InstanceRevision;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.deployment.DeploymentManager;
import io.cattle.platform.servicediscovery.upgrade.UpgradeManager;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

@Named
public class ServiceRollback extends AbstractDefaultProcessHandler {

    @Inject
    JsonMapper jsonMapper;
    @Inject
    UpgradeManager upgradeManager;
    @Inject
    ActivityService activityService;
    @Inject
    ServiceDao serviceDao;
    @Inject
    DeploymentManager deploymentMgr;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        final Service service = (Service) state.getResource();
        final io.cattle.platform.core.addon.ServiceUpgrade upgrade = DataAccessor.field(service,
                ServiceConstants.FIELD_UPGRADE, jsonMapper,
                io.cattle.platform.core.addon.ServiceUpgrade.class);
        
        ServiceUpgradeStrategy strategy = null;
        boolean finishUpgrade = false;
        if (upgrade != null) {
            strategy = upgrade.getStrategy();
            finishUpgrade = true;
        } else if (service.getRevisionId() != null) {
            final io.cattle.platform.core.addon.ServiceRollback rollback = jsonMapper.convertValue(state.getData(),
                    io.cattle.platform.core.addon.ServiceRollback.class);
            Pair<InstanceRevision, InstanceRevision> currentPreviousRevision = null;
            if (rollback != null && !StringUtils.isEmpty(rollback.getRevisionId())) {
                InstanceRevision currentRevision = serviceDao.getCurrentRevision(service);
                InstanceRevision previousRevision = serviceDao.getRevision(service,
                        Long.valueOf(rollback.getRevisionId()));
                currentPreviousRevision = Pair.of(currentRevision, previousRevision);
            } else {
                currentPreviousRevision = serviceDao
                        .getCurrentAndPreviousRevisions(service);
            }

            if (currentPreviousRevision == null || currentPreviousRevision.getRight() == null) {
                return null;
            }
            strategy = ServiceDiscoveryUtil.getStrategy(service, currentPreviousRevision, false);
        }

        final ServiceUpgradeStrategy finalStrategy = strategy;
        final boolean finalFinishUpgrade = finishUpgrade;

        if (finalStrategy != null) {
            activityService.run(service, "service.rollback", "Rolling back service", new Runnable() {
                @Override
                public void run() {
                    upgradeManager.rollback(service, finalStrategy);
                    if (finalFinishUpgrade) {
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