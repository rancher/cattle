package io.github.ibuildthecloud.dstack.framework.event;

import io.github.ibuildthecloud.dstack.eventing.model.EventVO;

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
