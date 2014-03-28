package io.cattle.platform.util.concurrent;

import io.cattle.platform.util.type.Named;

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
