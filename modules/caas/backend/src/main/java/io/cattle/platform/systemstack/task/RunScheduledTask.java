package io.cattle.platform.systemstack.task;

import io.cattle.platform.systemstack.service.UpgradeManager;
import io.cattle.platform.task.Task;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunScheduledTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(RunScheduledTask.class);

    UpgradeManager upgradeManager;

    public RunScheduledTask(UpgradeManager upgradeManager) {
        super();
        this.upgradeManager = upgradeManager;
    }

    @Override
    public void run() {
        try {
            upgradeManager.run();
        } catch (IOException e) {
            log.error("Failed to check for upgrades", e);
        }
    }

    @Override
    public String getName() {
        return "run.scheduled";
    }

}
