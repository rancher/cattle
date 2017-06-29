package io.cattle.platform.framework.event;

import io.cattle.platform.eventing.model.EventVO;

public class ExecuteTask extends EventVO<TaskOptions> {

    public ExecuteTask() {
        setName(FrameworkEvents.EXECUTE_TASK);
    }

    public ExecuteTask(String task) {
        this();

        TaskOptions options = new TaskOptions();
        options.setName(task);
        setData(options);
    }

}
