package io.cattle.platform.core.cleanup;

import io.cattle.platform.task.AbstractSingletonTask;

import javax.inject.Inject;

public class TableCleanUpCaller extends AbstractSingletonTask {

    @Inject
    TableCleanUp tableCleanUp;

    @Override
    protected void doRun() {
        tableCleanUp.cleanUp();
    }

    @Override
    public String getName() {
        return "database.cleanup";
    }
}
