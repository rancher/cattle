package io.cattle.platform.engine.manager.impl;

import io.cattle.platform.engine.server.ProcessInstanceReference;

import java.util.List;

public interface ProcessRecordDao {

    ProcessRecord insert(ProcessRecord record);

    void update(ProcessRecord record, boolean schedule);

    List<ProcessInstanceReference> pendingTasks();

    Long nextTask(String resourceType, String resourceId);

    ProcessRecord getRecord(Long id);

    ProcessInstanceReference loadReference(Long id);

}
