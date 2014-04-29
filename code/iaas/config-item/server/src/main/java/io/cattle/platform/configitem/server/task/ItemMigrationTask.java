package io.cattle.platform.configitem.server.task;

public class ItemMigrationTask extends AbstractItemSyncTask {

    @Override
    public String getName() {
        return "config.item.migration";
    }

    @Override
    protected boolean isMigration() {
        return true;
    }

}