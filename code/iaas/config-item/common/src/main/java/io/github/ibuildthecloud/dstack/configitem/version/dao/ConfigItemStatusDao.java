package io.github.ibuildthecloud.dstack.configitem.version.dao;

import io.github.ibuildthecloud.dstack.configitem.model.Client;
import io.github.ibuildthecloud.dstack.configitem.model.ItemVersion;
import io.github.ibuildthecloud.dstack.configitem.request.ConfigUpdateRequest;
import io.github.ibuildthecloud.dstack.core.model.ConfigItemStatus;

import java.util.List;

public interface ConfigItemStatusDao {

    Long getRequestedVersion(Client client, String itemName);

    long incrementOrApply(Client client, String itemName);

    boolean setApplied(Client client, String itemName, ItemVersion version);

    void setLatest(Client client, String itemName, String sourceRevision);

    boolean isAssigned(Client client, String itemName);

    void setItemSourceVersion(String name, String sourceRevision);

    List<? extends ConfigItemStatus> listItems(ConfigUpdateRequest request);

    ItemVersion getRequestedItemVersion(Client client, String itemName);

}
