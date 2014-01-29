package io.github.ibuildthecloud.dstack.task.cleanup;

import javax.inject.Inject;

import io.github.ibuildthecloud.dstack.task.Task;
import io.github.ibuildthecloud.dstack.task.dao.impl.TaskDaoImpl;

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
