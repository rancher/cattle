package io.cattle.platform.configitem.server.task;

public class ItemSyncTask extends AbstractItemSyncTask {

    @Override
    public String getName() {
        return "config.item.sync";
    }

    @Override
    protected boolean isMigration() {
        return false;
    }

}