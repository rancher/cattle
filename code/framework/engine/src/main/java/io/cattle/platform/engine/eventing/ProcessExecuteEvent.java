package io.cattle.platform.engine.eventing;

import io.cattle.platform.eventing.model.EventVO;

public class ProcessExecuteEvent extends EventVO<ProcessExecuteEventData> {

    public ProcessExecuteEvent() {
        setName(EngineEvents.PROCESS_EXECUTE2);
    }

    public ProcessExecuteEvent(Long processId, String name, String resourceType, String resourceId, Long accountId) {
        this();
        setData(new ProcessExecuteEventData(name, resourceType, resourceId, accountId));
        setResourceId(processId.toString());
    }
}
