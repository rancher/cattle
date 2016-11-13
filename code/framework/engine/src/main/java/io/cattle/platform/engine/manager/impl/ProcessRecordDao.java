package io.cattle.platform.engine.manager.impl;

import java.util.List;

public interface ProcessRecordDao {

    ProcessRecord insert(ProcessRecord record);

    void update(ProcessRecord record, boolean schedule);

    List<Long> pendingTasks(String resourceType, String resourceId, boolean priority);

    ProcessRecord getRecord(Long id);

}
