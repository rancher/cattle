package io.github.ibuildthecloud.dstack.configitem.events;

import io.github.ibuildthecloud.dstack.configitem.request.ConfigUpdateItem;
import io.github.ibuildthecloud.dstack.eventing.model.EventVO;
import io.github.ibuildthecloud.dstack.iaas.event.IaasEvents;

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
