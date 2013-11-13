package io.github.ibuildthecloud.dstack.engine.repository.impl;

import java.util.List;


public interface ProcessRecordDao {

    void update(ProcessRecord record);

    List<Long> pendingTasks();

    ProcessRecord getRecord(Long id);

}
