package io.cattle.platform.core.cleanup;

import io.cattle.platform.core.dao.impl.TaskDaoImpl;
import io.cattle.platform.task.Task;

import javax.inject.Inject;

public class CleanupTaskInstances implements Task {

    TaskDaoImpl taskDao;

    @Override
    public void run() {
        taskDao.purgeOld();
    }

    @Override
    public String getName() {
        return "cleanup.task.instances";
    }

    public TaskDaoImpl getTaskDao() {
        return taskDao;
    }

    @Inject
    public void setTaskDao(TaskDaoImpl taskDao) {
        this.taskDao = taskDao;
    }

}
