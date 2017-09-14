package io.cattle.platform.systemstack.task;

import io.cattle.platform.systemstack.service.UpgradeManager;
import io.cattle.platform.task.Task;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpgradeScheduleTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(UpgradeScheduleTask.class);

    UpgradeManager upgradeManager;

    public UpgradeScheduleTask(UpgradeManager upgradeManager) {
        this.upgradeManager = upgradeManager;
    }

    @Override
    public void run() {
        try {
            upgradeManager.schedule();
        } catch (IOException e) {
            log.error("Failed to check for upgrades", e);
        }
    }

    @Override
    public String getName() {
        return "upgrade.schedule";
    }

}
