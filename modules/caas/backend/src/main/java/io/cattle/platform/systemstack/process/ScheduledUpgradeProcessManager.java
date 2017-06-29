package io.cattle.platform.systemstack.process;

import static io.cattle.platform.core.model.tables.ScheduledUpgradeTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.ScheduledUpgrade;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessDelayException;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.systemstack.catalog.CatalogService;
import io.cattle.platform.systemstack.service.UpgradeManager;

import java.io.IOException;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.netflix.config.DynamicLongProperty;

public class ScheduledUpgradeProcessManager {

    public static final DynamicLongProperty DEFAULT_DELAY = ArchaiusUtil.getLong("default.schedule.upgrade.delay.minutes");

    CatalogService catalogService;
    UpgradeManager upgradeManager;
    ObjectManager objectManager;
    ObjectProcessManager processManager;

    public ScheduledUpgradeProcessManager(CatalogService catalogService, UpgradeManager upgradeManager, ObjectManager objectManager,
            ObjectProcessManager processManager) {
        super();
        this.catalogService = catalogService;
        this.upgradeManager = upgradeManager;
        this.objectManager = objectManager;
        this.processManager = processManager;
    }

    public HandlerResult create(ProcessState state, ProcessInstance process) {
        ScheduledUpgrade upgrade = (ScheduledUpgrade)state.getResource();
        Stack stack = objectManager.loadResource(Stack.class, upgrade.getStackId());
        if (stack.getRemoved() != null) {
            return new HandlerResult()
                    .withChainProcessName(processManager.getStandardProcessName(StandardProcess.REMOVE, upgrade));
        }
        Account account = objectManager.loadResource(Account.class, upgrade.getAccountId());
        Long delay = DataAccessor.fieldLong(account, AccountConstants.FIELD_SCHEDULED_UPGRADE_DELAY);
        if (delay == null) {
            delay = DEFAULT_DELAY.get();
        }
        if (delay < 0) {
            delay = 26280000L;
        }
        Long priority = 0L;
        if (StringUtils.isNotBlank(stack.getExternalId()) && stack.getExternalId().contains(UpgradeManager.METADATA)) {
            priority = 100L;
            delay = 0L;
        }
        Date runAfter = new Date(upgrade.getCreated().getTime() + delay * 60000);
        HandlerResult result = new HandlerResult(SCHEDULED_UPGRADE.RUN_AFTER, runAfter,
                SCHEDULED_UPGRADE.PRIORITY, priority);
        if (delay == 0L) {
            DeferredUtils.defer(new Runnable() {
                @Override
                public void run() {
                    try {
                        upgradeManager.run();
                    } catch (Exception e) {
                    }
                }
            });
        }
        return result;
    }


    public HandlerResult start(ProcessState state, ProcessInstance process) {
        ScheduledUpgrade upgrade = (ScheduledUpgrade)state.getResource();
        try {
            process(state, process);
            if (upgrade.getFinished() == null) {
                objectManager.setFields(upgrade, SCHEDULED_UPGRADE.FINISHED, new Date());
                objectManager.reload(upgrade);
            }
            upgradeManager.run();
            return new HandlerResult()
                    .withChainProcessName(processManager.getStandardProcessName(StandardProcess.REMOVE, upgrade));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void process(ProcessState state, ProcessInstance process) throws IOException {
        ScheduledUpgrade upgrade = (ScheduledUpgrade)state.getResource();
        Stack stack = objectManager.loadResource(Stack.class, upgrade.getStackId());
        if (StringUtils.isBlank(stack.getExternalId()) || stack.getRemoved() != null) {
            return;
        }

        if (ServiceConstants.STATE_UPGRADED.equals(stack.getState())) {
            processManager.scheduleProcessInstance(ServiceConstants.PROCESS_STACK_FINISH_UPGRADE, stack, null);
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
                String setting = UpgradeManager.UPGRADE_MANAGER.get();
                if (setting.equalsIgnoreCase("manadatory") && !stack.getExternalId().contains(UpgradeManager.METADATA)) {
                    return;
                }
                if (!setting.equalsIgnoreCase("true") && !setting.equalsIgnoreCase("all")
                        && !setting.equalsIgnoreCase("mandatory")) {
                    return;
                }
                catalogService.upgrade(stack);
                objectManager.setFields(upgrade, SCHEDULED_UPGRADE.STARTED, new Date());
            }
        }

        throw new ProcessDelayException(new Date(System.currentTimeMillis() + 15000));
    }




}
