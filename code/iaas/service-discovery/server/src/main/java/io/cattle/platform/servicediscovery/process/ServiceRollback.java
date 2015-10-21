package io.cattle.platform.servicediscovery.process;

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

public class ServiceRollback extends AbstractDefaultProcessHandler {

    @Inject
    JsonMapper jsonMapper;

    @Inject
    UpgradeManager upgradeManager;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Service service = (Service) state.getResource();
        io.cattle.platform.core.addon.ServiceUpgrade upgrade = DataAccessor.field(service,
                ServiceDiscoveryConstants.FIELD_UPGRADE, jsonMapper,
                io.cattle.platform.core.addon.ServiceUpgrade.class);

        if (upgrade != null) {
            upgradeManager.rollback(service, upgrade.getStrategy());
            upgradeManager.finishUpgrade(service, true);
        }

        return null;
    }
}