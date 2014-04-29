package io.cattle.platform.configitem.version.dao;

import io.cattle.platform.configitem.model.Client;
import io.cattle.platform.configitem.model.ItemVersion;
import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.core.model.ConfigItemStatus;

import java.util.List;
import java.util.Map;

public interface ConfigItemStatusDao {

    Long getRequestedVersion(Client client, String itemName);

    long incrementOrApply(Client client, String itemName);

    boolean setApplied(Client client, String itemName, ItemVersion version);

    void setLatest(Client client, String itemName, String sourceRevision);

    boolean isAssigned(Client client, String itemName);

    void setItemSourceVersion(String name, String sourceRevision);

    List<? extends ConfigItemStatus> listItems(ConfigUpdateRequest request);

    ItemVersion getRequestedItemVersion(Client client, String itemName);

    Map<Long,List<String>> findOutOfSync(boolean migration);

}
