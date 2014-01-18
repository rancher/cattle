package io.github.ibuildthecloud.dstack.configitem.server.model;

import java.io.IOException;
import java.util.Collection;

public interface ConfigItemFactory {

    Collection<ConfigItem> getConfigItems() throws IOException;

}
