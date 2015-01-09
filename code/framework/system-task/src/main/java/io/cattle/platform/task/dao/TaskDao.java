package io.cattle.platform.task.dao;

import io.cattle.platform.task.Task;

public interface TaskDao {

    void register(String name);

    Object newRecord(Task task);

    void finish(Object record);

    void failed(Object record, Throwable t);

}
