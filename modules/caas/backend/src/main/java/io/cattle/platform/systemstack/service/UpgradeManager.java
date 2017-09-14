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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicStringProperty;

public class UpgradeManager {

    private static final DynamicIntProperty MAX_UPGRADE = ArchaiusUtil.getInt("concurrent.scheduled.upgrades");
    public static final DynamicStringProperty UPGRADE_MANAGER = ArchaiusUtil.getString("upgrade.manager");
    public static final String METADATA = "library:infra*network-services";
    private static final DynamicBooleanProperty LAUNCH_CATALOG = ArchaiusUtil.getBoolean("catalog.execute");
    private static final Logger log = LoggerFactory.getLogger(UpgradeManager.class);
    private static final Set<String> OLD_METADATAS = new HashSet<>(Arrays.asList(
            "catalog://library:infra*network-services:0",
            "catalog://library:infra*network-services:1",
            "catalog://library:infra*network-services:2",
            "catalog://library:infra*network-services:3",
            "catalog://library:infra*network-services:4",
            "catalog://library:infra*network-services:5",
            "catalog://library:infra*network-services:6",
            "catalog://library:infra*network-services:7",
            "catalog://library:infra*network-services:8",
            "catalog://library:infra*network-services:9",
            "catalog://library:infra*network-services:11",
            "catalog://library:infra*network-services:12",
            "catalog://library:infra*network-services:13",
            "catalog://library:infra*network-services:14",
            "catalog://library:infra*network-services:18"
            ));

    CatalogService catalogService;
    StackDao stackDao;
    GenericResourceDao resourceDao;
    LockManager lockManager;
    ObjectProcessManager processManager;

    public UpgradeManager(CatalogService catalogService, StackDao stackDao, GenericResourceDao resourceDao, LockManager lockManager,
            ObjectProcessManager processManager) {
        this.catalogService = catalogService;
        this.stackDao = stackDao;
        this.resourceDao = resourceDao;
        this.lockManager = lockManager;
        this.processManager = processManager;
    }

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

        if (catalogs.size() == 0) {
            return;
        }

        List<? extends Stack> stacks = Collections.emptyList();

        if ("true".equalsIgnoreCase(UPGRADE_MANAGER.get()) || "all".equalsIgnoreCase(UPGRADE_MANAGER.get())) {
            stacks = stackDao.getStacksToUpgrade(catalogs.values());
        } else if ("mandatory".equalsIgnoreCase(UPGRADE_MANAGER.get())) {
            stacks = stackDao.getStacksThatMatch(OLD_METADATAS);
        }

        for (Stack stack : stacks) {
            String templateId = catalogService.getTemplateIdFromExternalId(stack.getExternalId());
            if (StringUtils.isBlank(templateId)) {
                continue;
            }

            if (catalogs.containsKey(templateId)) {
                resourceDao.createAndSchedule(ScheduledUpgrade.class,
                        SCHEDULED_UPGRADE.ACCOUNT_ID, stack.getAccountId(),
                        SCHEDULED_UPGRADE.STACK_ID, stack.getId());
            }
        }
    }

    public void run() throws IOException {
        lockManager.tryLock(new ScheduledUpgradeLock(), ()->{
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
