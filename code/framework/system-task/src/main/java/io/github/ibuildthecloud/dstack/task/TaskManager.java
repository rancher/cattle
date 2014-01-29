package io.github.ibuildthecloud.dstack.task;

public interface TaskManager {

    void execute(String name);

    Runnable getRunnable(String name);

}
