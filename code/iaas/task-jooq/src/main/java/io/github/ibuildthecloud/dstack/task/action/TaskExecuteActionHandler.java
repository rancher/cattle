package io.github.ibuildthecloud.dstack.task.action;

import javax.inject.Inject;

import io.github.ibuildthecloud.dstack.api.action.ActionHandler;
import io.github.ibuildthecloud.dstack.core.model.Task;
import io.github.ibuildthecloud.dstack.task.TaskManager;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

public class TaskExecuteActionHandler implements ActionHandler {

    TaskManager taskManager;

    @Override
    public String getName() {
        return "task.execute";
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        Task task = (Task)obj;
        taskManager.execute(task.getName());
        return task;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    @Inject
    public void setTaskManager(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

}
