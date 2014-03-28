package io.cattle.platform.configitem.server.model;

import java.io.IOException;
import java.util.Collection;

public interface ConfigItemFactory {

    Collection<ConfigItem> getConfigItems() throws IOException;

}
