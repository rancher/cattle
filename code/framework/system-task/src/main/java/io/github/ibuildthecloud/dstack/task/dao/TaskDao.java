package io.github.ibuildthecloud.dstack.task.dao;


public interface TaskDao {

    void register(String name);

    Object newRecord(String name);

    void finish(Object record);

    void failed(Object record, Throwable t);

}
