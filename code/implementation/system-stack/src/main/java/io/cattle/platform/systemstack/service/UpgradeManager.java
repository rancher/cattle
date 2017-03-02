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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicIntProperty;

public class UpgradeManager {

    private static final DynamicIntProperty MAX_UPGRADE = ArchaiusUtil.getInt("concurrent.scheduled.upgrades");
    public static final DynamicBooleanProperty UPGRADE_MANAGER = ArchaiusUtil.getBoolean("upgrade.manager");
    public static final String METADATA = "library:infra*network-services";
    private static final DynamicBooleanProperty LAUNCH_CATALOG = ArchaiusUtil.getBoolean("catalog.execute");
    private static final Logger log = LoggerFactory.getLogger(UpgradeManager.class);

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
        if (!LAUNCH_CATALOG.get()) {
            return;
        }

        Map<String, String> catalogs = null;
        while (true) {
            try {
                catalogs = catalogService.latestInfraTemplates();
                break;
            } catch (IOException e) {
                log.info("Waiting for catalog service");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e1) {
                    throw new IllegalStateException(e1);
                }
            }
        }

        if (!UPGRADE_MANAGER.get()) {
            Map<String, String> temp = new HashMap<>();
            String value = catalogs.get(METADATA);
            if (value != null) {
                temp.put(METADATA, value);
            }
            catalogs = temp;
        }

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
