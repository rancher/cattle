package io.cattle.platform.systemstack.process;

import static io.cattle.platform.core.model.tables.ScheduledUpgradeTable.*;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.ScheduledUpgrade;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessDelayException;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.systemstack.catalog.CatalogService;
import io.cattle.platform.systemstack.service.UpgradeManager;

import java.io.IOException;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;


@Named
public class ScheduledUpgradeStart extends AbstractDefaultProcessHandler {

    @Inject
    CatalogService catalogService;
    @Inject
    UpgradeManager upgradeManager;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        ScheduledUpgrade upgrade = (ScheduledUpgrade)state.getResource();
        try {
            process(state, process);
            if (upgrade.getFinished() == null) {
                objectManager.setFields(upgrade, SCHEDULED_UPGRADE.FINISHED, new Date());
                objectManager.reload(upgrade);
            }
            upgradeManager.run();
            return new HandlerResult()
                    .withChainProcessName(objectProcessManager.getStandardProcessName(StandardProcess.REMOVE, upgrade));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void process(ProcessState state, ProcessInstance process) throws IOException {
        ScheduledUpgrade upgrade = (ScheduledUpgrade)state.getResource();
        Stack stack = objectManager.loadResource(Stack.class, upgrade.getStackId());
        if (StringUtils.isBlank(stack.getExternalId())) {
            return;
        }

        if (ServiceConstants.STATE_UPGRADED.equals(stack.getState())) {
            objectProcessManager.scheduleProcessInstance(ServiceConstants.PROCESS_STACK_FINISH_UPGRADE, stack, null);
            throw new ProcessDelayException(new Date(System.currentTimeMillis() + 15000));
        }

        if (CommonStatesConstants.ACTIVE.equals(stack.getState())) {
            String upgradeToExternalId = catalogService.getDefaultExternalId(stack);
            if (StringUtils.isBlank(upgradeToExternalId)) {
                return;
            }

            if (upgradeToExternalId.equals(stack.getExternalId())) {
                return;
            } else {
                if (!UpgradeManager.UPGRADE_MANAGER.get() && !stack.getExternalId().contains(UpgradeManager.METADATA)) {
                    return;
                }
                catalogService.upgrade(stack);
                objectManager.setFields(upgrade, SCHEDULED_UPGRADE.STARTED, new Date());
            }
        }

        throw new ProcessDelayException(new Date(System.currentTimeMillis() + 15000));
    }

}
