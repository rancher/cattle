package io.cattle.platform.configitem.context;

import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.Request;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;

public interface ConfigItemContextFactory {

    String[] getItems();

    void populateContext(Request req, ConfigItem item, ArchiveContext context);

}
