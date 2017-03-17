package io.cattle.platform.systemstack.process;

import static io.cattle.platform.core.model.tables.ScheduledUpgradeTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.ScheduledUpgrade;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.systemstack.service.UpgradeManager;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

import com.netflix.config.DynamicLongProperty;


@Named
public class ScheduledUpgradeCreate extends AbstractDefaultProcessHandler {

    public static final DynamicLongProperty DEFAULT_DELAY = ArchaiusUtil.getLong("default.schedule.upgrade.delay.minutes");

    @Inject
    UpgradeManager upgradeManager;

    @Inject
    ObjectProcessManager processManager;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
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

}
