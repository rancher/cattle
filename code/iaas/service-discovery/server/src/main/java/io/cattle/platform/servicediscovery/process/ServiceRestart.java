package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.upgrade.UpgradeManager;

import javax.inject.Inject;

public class ServiceRestart extends AbstractDefaultProcessHandler {

    @Inject
    JsonMapper jsonMapper;

    @Inject
    UpgradeManager upgradeManager;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Service service = (Service) state.getResource();
        io.cattle.platform.core.addon.ServiceRestart restart = jsonMapper.convertValue(state.getData(),
                io.cattle.platform.core.addon.ServiceRestart.class);

        objectManager.setFields(service, ServiceDiscoveryConstants.FIELD_RESTART, restart);

        upgradeManager.restart(service, restart.getRollingRestartStrategy());

        return null;
    }
}
