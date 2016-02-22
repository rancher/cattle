package io.cattle.platform.configitem.events;

import io.cattle.platform.configitem.request.ConfigUpdateItem;
import io.cattle.platform.eventing.model.EventVO;

import java.util.List;

public class ConfigUpdate extends EventVO<ConfigUpdateData> {


    /**
     * Do not use this constructor, only used for JSON unmarshalling
     */
    public ConfigUpdate() {
    }

    public ConfigUpdate(String eventName, String url, List<ConfigUpdateItem> items) {
        setName(eventName);
        ConfigUpdateData updateData = new ConfigUpdateData();
        updateData.setItems(items);
        updateData.setConfigUrl(url);

        setData(updateData);
    }

    public ConfigUpdate(ConfigUpdate configUpdate, List<ConfigUpdateItem> items) {
        super(configUpdate, null);

        ConfigUpdateData updateData = new ConfigUpdateData();
        updateData.setItems(items);
        updateData.setConfigUrl(configUpdate.getData().getConfigUrl());

        setData(updateData);
    }

}
