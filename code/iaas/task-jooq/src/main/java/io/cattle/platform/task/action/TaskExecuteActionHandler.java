package io.cattle.platform.task.action;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.model.Task;
import io.cattle.platform.task.TaskManager;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import javax.inject.Inject;

public class TaskExecuteActionHandler implements ActionHandler {

    TaskManager taskManager;

    @Override
    public String getName() {
        return "task.execute";
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        Task task = (Task) obj;
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
