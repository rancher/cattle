package io.cattle.platform.configitem.version.impl;

import io.cattle.platform.configitem.model.Client;
import io.cattle.platform.lock.definition.AbstractLockDefinition;

public class ConfigItemProcessLock extends AbstractLockDefinition {

    public ConfigItemProcessLock(String item, Client client) {
        super(String.format("CONFIG.ITEM.PROCESS.%s.%s.%d.%s", client.getEventName(), client.getResourceType(),
                client.getResourceId(), item));
    }

}