package io.github.ibuildthecloud.dstack.configitem.events;

import io.github.ibuildthecloud.dstack.configitem.request.ConfigUpdateItem;

import java.util.List;

public class ConfigUpdateData {

    String configUrl;
    List<ConfigUpdateItem> items;

    public List<ConfigUpdateItem> getItems() {
        return items;
    }

    public void setItems(List<ConfigUpdateItem> items) {
        this.items = items;
    }

    public String getConfigUrl() {
        return configUrl;
    }

    public void setConfigUrl(String configUrl) {
        this.configUrl = configUrl;
    }

}
