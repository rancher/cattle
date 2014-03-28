package io.cattle.platform.configitem.events;

import io.cattle.platform.configitem.request.ConfigUpdateItem;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.iaas.event.IaasEvents;

import java.util.List;

public class ConfigUpdate extends EventVO<ConfigUpdateData> {

    public ConfigUpdate() {
        setName(IaasEvents.CONFIG_UPDATE);
    }

    public ConfigUpdate(String url, List<ConfigUpdateItem> items) {
        this();
        ConfigUpdateData updateData = new ConfigUpdateData();
        updateData.setItems(items);
        updateData.setConfigUrl(url);

        setData(updateData);
    }

}
