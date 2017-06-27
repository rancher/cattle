package io.cattle.platform.core.cleanup;

import io.cattle.platform.task.Task;
import io.cattle.platform.task.dao.TaskDao;

public class CleanupTaskInstances implements Task {

    TaskDao taskDao;

    public CleanupTaskInstances(TaskDao taskDao) {
        this.taskDao = taskDao;
    }

    @Override
    public void run() {
        taskDao.purgeOld();
    }

    @Override
    public String getName() {
        return "cleanup.task.instances";
    }

}
