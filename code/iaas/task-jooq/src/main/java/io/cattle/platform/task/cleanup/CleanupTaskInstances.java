package io.cattle.platform.task.cleanup;

import io.cattle.platform.task.Task;
import io.cattle.platform.task.dao.impl.TaskDaoImpl;

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
