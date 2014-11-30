package org.apache.cloudstack.configitem.server.model.impl;

import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.ConfigItemFactory;
import io.cattle.platform.configitem.server.model.Request;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

public class WriteStringConfigItem implements ConfigItem, ConfigItemFactory {

    String name;
    String content;

    public WriteStringConfigItem(String name, String content) {
        super();
        this.name = name;
        this.content = content;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void handleRequest(Request req) throws IOException {
        req.getOutputStream().write(content.getBytes("UTF-8"));
    }

    @Override
    public String getSourceRevision() {
        return name + "/" + content;
    }

    @Override
    public Collection<ConfigItem> getConfigItems() {
        return Arrays.asList((ConfigItem)this);
    }

    @Override
    public boolean isDynamicallyApplied() {
        return false;
    }

}
