package io.github.ibuildthecloud.dstack.task.dao;

import io.github.ibuildthecloud.dstack.task.Task;


public interface TaskDao {

    void register(String name);

    Object newRecord(Task task);

    void finish(Object record);

    void failed(Object record, Throwable t);

}
