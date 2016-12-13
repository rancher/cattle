package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.StoragePoolHostMap;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.util.List;
import java.util.Map;

public interface StoragePoolDao {

    List<? extends StoragePool> findBadPools(int limit);

    List<? extends StoragePoolHostMap> findBadPoolMapss(int limit);

    List<? extends StoragePool> findExternalActivePools();

    StoragePool mapNewPool(Host host, Map<String, Object> properties);

    StoragePool mapNewPool(Long hostId, Map<String, Object> properties);

    List<? extends StoragePool> findStoragePoolByDriverName(Long accountId, String driverName);

    Map<Long, Long> findStoragePoolHostsByDriver(Long accountId, Long storageDriverId);

    List<? extends StoragePool> findNonRemovedStoragePoolByDriver(Long storageDriverId);

    List<? extends StoragePoolHostMap> findMapsToRemove(Long id);

    StoragePoolHostMap findNonremovedMap(Long storagePoolId, Long hostId);

    void createStoragePoolHostMap(StoragePoolHostMap m);

    List<Long> findVolumesInUseByServiceDriver(Long serviceId);

    StoragePool associateVolumeToPool(Long volumeId, Long storageDriverId, Long hostId);

    Map<Long, List<Object>> findHostsForPools(List<Long> ids, IdFormatter idFormatter);

    Map<Long, List<Object>> findVolumesForPools(List<Long> ids, IdFormatter idFormatter);

    List<? extends StoragePool> findBadDockerPools(int count);

}
