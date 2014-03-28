package io.cattle.platform.configitem.events;

import io.cattle.platform.configitem.request.ConfigUpdateItem;

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
