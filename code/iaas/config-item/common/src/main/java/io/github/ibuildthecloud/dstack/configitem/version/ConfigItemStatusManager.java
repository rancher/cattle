package io.github.ibuildthecloud.dstack.configitem.version;

import io.github.ibuildthecloud.dstack.configitem.model.Client;
import io.github.ibuildthecloud.dstack.configitem.model.ItemVersion;

public interface ConfigItemStatusManager {

    boolean setApplied(Client client, String itemName, ItemVersion version);

    void setLatest(Client client, String itemName, String sourceRevision);

    void setItemSourceVersion(String name, String sourceRevision);

    boolean isAssigned(Client client, String itemName);

}
