package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.StoragePool;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.util.List;
import java.util.Map;

public interface StoragePoolDao {

    StoragePool mapNewPool(Host host, Map<String, Object> properties);

    StoragePool mapNewPool(Long hostId, Map<String, Object> properties);

    void mapPoolToHost(Long storagePoolId, Long hostId);

    List<? extends StoragePool> findStoragePoolByDriverName(long clusterId, String driverName);

    Map<Long, Long> findStoragePoolHostsByDriver(long clusterId, Long storageDriverId);

    List<? extends StoragePool> findNonRemovedStoragePoolByDriver(Long storageDriverId);

    List<? extends StoragePool> findNonRemovedStoragePoolByHost(long hostId);

    List<? extends StoragePool> findPoolsForHost(long hostId);

    List<Long> findVolumesInUseByServiceDriver(Long serviceId);

    StoragePool associateVolumeToPool(Long volumeId, Long storageDriverId, Long hostId);

    Map<Long, List<Object>> findHostsForPools(List<Long> ids, IdFormatter idFormatter);

    Map<Long, List<Object>> findVolumesForPools(List<Long> ids, IdFormatter idFormatter);

}
