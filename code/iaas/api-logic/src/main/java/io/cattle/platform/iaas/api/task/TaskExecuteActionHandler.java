package io.cattle.platform.iaas.api.task;

import io.cattle.platform.core.model.Task;
import io.cattle.platform.task.TaskManager;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;

public class TaskExecuteActionHandler implements ActionHandler {

    TaskManager taskManager;

    public TaskExecuteActionHandler(TaskManager taskManager) {
        super();
        this.taskManager = taskManager;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        Task task = (Task) obj;
        taskManager.execute(task.getName());
        return task;
    }

}
