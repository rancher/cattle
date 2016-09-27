package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.StoragePoolHostMap;

import java.util.List;
import java.util.Map;

public interface StoragePoolDao {

    List<? extends StoragePool> findExternalActivePools();

    StoragePool mapNewPool(Host host, Map<String, Object> properties);

    List<? extends StoragePool> findStoragePoolByDriverName(Long accountId, String driverName);

    List<? extends StoragePoolHostMap> findMapsToRemove(Long id);

    StoragePoolHostMap findNonremovedMap(Long storagePoolId, Long hostId);

    void createStoragePoolHostMap(StoragePoolHostMap m);
}
