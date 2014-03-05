package io.github.ibuildthecloud.dstack.util.concurrent;

import io.github.ibuildthecloud.dstack.util.type.Named;

import java.util.concurrent.ExecutorService;

public class NamedExecutorService implements Named {

    ExecutorService executorService;
    String name;

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
