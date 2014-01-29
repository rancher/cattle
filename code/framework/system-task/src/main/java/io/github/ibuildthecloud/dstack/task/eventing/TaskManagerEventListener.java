package io.github.ibuildthecloud.dstack.task.eventing;

import io.github.ibuildthecloud.dstack.eventing.annotation.AnnotatedEventListener;
import io.github.ibuildthecloud.dstack.eventing.annotation.EventHandler;
import io.github.ibuildthecloud.dstack.framework.event.ExecuteTask;

public interface TaskManagerEventListener extends AnnotatedEventListener {

    @EventHandler
    void executeTask(ExecuteTask event);

}
