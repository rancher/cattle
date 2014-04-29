package io.cattle.platform.configitem.version;

import com.google.common.util.concurrent.ListenableFuture;

import io.cattle.platform.configitem.model.Client;
import io.cattle.platform.configitem.model.ItemVersion;
import io.cattle.platform.configitem.request.ConfigUpdateRequest;

public interface ConfigItemStatusManager {

    ItemVersion getRequestedVersion(Client client, String itemName);

    boolean setApplied(Client client, String itemName, ItemVersion version);

    void setLatest(Client client, String itemName, String sourceRevision);

    void setItemSourceVersion(String name, String sourceRevision);

    boolean isAssigned(Client client, String itemName);

    void updateConfig(ConfigUpdateRequest request);

    ListenableFuture<?> whenReady(ConfigUpdateRequest request);

    void waitFor(ConfigUpdateRequest request);

    void sync(boolean migration);

}
