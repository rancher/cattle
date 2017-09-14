package io.cattle.platform.task.eventing;

import io.cattle.platform.eventing.annotation.AnnotatedEventListener;
import io.cattle.platform.eventing.annotation.EventHandler;
import io.cattle.platform.framework.event.ExecuteTask;

public interface TaskManagerEventListener extends AnnotatedEventListener {

    @EventHandler
    void executeTask(ExecuteTask event);

}
