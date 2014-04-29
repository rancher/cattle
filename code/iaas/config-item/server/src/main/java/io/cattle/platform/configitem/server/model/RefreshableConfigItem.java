package io.cattle.platform.configitem.server.model;

import java.io.IOException;

public interface RefreshableConfigItem {

    boolean refresh() throws IOException;

}
