package io.cattle.platform.engine.eventing;

import io.cattle.platform.engine.manager.impl.ProcessRecord;
import io.cattle.platform.eventing.model.EventVO;

public class ProcessExecuteEvent extends EventVO<ProcessExecuteEventData, Object> {

    public ProcessExecuteEvent() {
        setName(EngineEvents.PROCESS_EXECUTE);
    }

    public ProcessExecuteEvent(ProcessRecord processRecord) {
        this(processRecord.getId(),
                processRecord.getProcessName(),
                processRecord.getResourceType(),
                processRecord.getResourceId(),
                processRecord.getAccountId(),
                processRecord.getClusterId());
    }

    public ProcessExecuteEvent(Long processId, String name, String resourceType, String resourceId, Long accountId, Long clusterId) {
        this();
        setData(new ProcessExecuteEventData(name, resourceType, resourceId, accountId, clusterId));
        setResourceId(processId.toString());
    }
}
