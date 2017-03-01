package io.cattle.platform.systemstack.service;

import static io.cattle.platform.core.model.tables.ScheduledUpgradeTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.StackDao;
import io.cattle.platform.core.model.ScheduledUpgrade;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.systemstack.catalog.CatalogService;
import io.cattle.platform.systemstack.lock.ScheduledUpgradeLock;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicIntProperty;

public class UpgradeManager {

    private static final DynamicIntProperty MAX_UPGRADE = ArchaiusUtil.getInt("concurrent.scheduled.upgrades");
    private static final DynamicBooleanProperty UPGRADE_MANAGER = ArchaiusUtil.getBoolean("upgrade.manager");

    @Inject
    CatalogService catalogService;
    @Inject
    StackDao stackDao;
    @Inject
    GenericResourceDao resourceDao;
    @Inject
    LockManager lockManager;
    @Inject
    ObjectProcessManager processManager;

    public void schedule() throws IOException {
        lockManager.lock(new ScheduledUpgradeLock(), ()->{
            try {
                scheduleWithLock();
                runWithLock(false);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            return null;
        });
    }

    protected void scheduleWithLock() throws IOException {
        if (!UPGRADE_MANAGER.get()) {
            return;
        }

        Map<String, String> catalogs = catalogService.latestInfraTemplates();
        if (catalogs.size() == 0) {
            return;
        }

        List<? extends Stack> stacks = stackDao.getStacksToUpgrade(catalogs.values());

        for (Stack stack : stacks) {
            String templateId = catalogService.getTemplateIdFromExternalId(stack.getExternalId());
            if (StringUtils.isBlank(templateId)) {
                continue;
            }

            if (catalogs.containsKey(templateId) && !stackDao.hasSkipServices(stack.getId())) {
                resourceDao.createAndSchedule(ScheduledUpgrade.class,
                        SCHEDULED_UPGRADE.ACCOUNT_ID, stack.getAccountId(),
                        SCHEDULED_UPGRADE.STACK_ID, stack.getId());
            }
        }
    }

    public void run() throws IOException {
        lockManager.lock(new ScheduledUpgradeLock(), ()->{
            try {
                runWithLock(true);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            return null;
        });
    }

    protected void runWithLock(boolean scheduleNext) throws IOException {
        if (!UPGRADE_MANAGER.get()) {
            return;
        }

        int max = MAX_UPGRADE.get();
        if (scheduleNext) {
            max++;
        }

        List<? extends ScheduledUpgrade> running = stackDao.getRunningUpgrades();
        if (running.size() >= max) {
            return;
        }

        Set<Long> runningAccounts = running.stream()
            .map((x)-> x.getAccountId())
            .collect(Collectors.toSet());

        List<? extends ScheduledUpgrade> toRun = stackDao.getReadyUpgrades(runningAccounts, max-running.size());
        for (ScheduledUpgrade upgrade : toRun) {
            processManager.scheduleProcessInstance("scheduledupgrade.start", upgrade, null);
        }
    }

}
