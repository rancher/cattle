package io.cattle.platform.engine.manager.impl;

import io.cattle.platform.engine.model.ProcessReference;

import java.util.List;

public interface ProcessRecordDao {

    ProcessRecord insert(ProcessRecord record);

    void update(ProcessRecord record, boolean schedule);

    List<ProcessReference> pendingTasks();

    ProcessRecord getRecord(Long id);

    void setDone(Object obj, String stateField, String state);

}
