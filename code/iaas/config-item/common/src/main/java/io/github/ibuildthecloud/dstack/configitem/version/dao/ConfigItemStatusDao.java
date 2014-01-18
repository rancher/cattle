package io.github.ibuildthecloud.dstack.configitem.version.dao;

import io.github.ibuildthecloud.dstack.configitem.model.Client;
import io.github.ibuildthecloud.dstack.configitem.model.ItemVersion;

public interface ConfigItemStatusDao {

    boolean setApplied(Client client, String itemName, ItemVersion version);

    void setLatest(Client client, String itemName, String sourceRevision);

    boolean isAssigned(Client client, String itemName);

    void setItemSourceVersion(String name, String sourceRevision);

}
