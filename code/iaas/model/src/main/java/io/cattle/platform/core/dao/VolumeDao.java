package io.cattle.platform.core.dao;

import io.cattle.platform.core.addon.MountEntry;
import io.cattle.platform.core.model.Image;
import io.cattle.platform.core.model.ImageStoragePoolMap;
import io.cattle.platform.core.model.Mount;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeStoragePoolMap;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface VolumeDao {
    List<? extends Volume> findBadVolumes(int count);

    List<? extends Mount> findBadMounts(int count);

    List<? extends VolumeStoragePoolMap> findBandVolumeStoragePoolMap(int count);

    Volume createVolumeForDriver(long accountId, String name, String volumeName);

    Volume findVolumeByExternalId(Long storagePoolId, String externalId);

    void createVolumeInStoragePool(Map<String, Object> volumeData, String volumeName, StoragePool storagePool);

    List<? extends Volume> findSharedOrUnmappedVolumes(long accountId, String volumeName);

    Set<? extends Volume> findNonremovedVolumesWithNoOtherMounts(long instanceId);

    boolean isVolumeInUseByRunningInstance(long volumeId);

    Map<Long, List<MountEntry>> getMountsForInstances(List<Long> ids, IdFormatter idF);

    Map<Long, List<MountEntry>> getMountsForVolumes(List<Long> ids, IdFormatter idF);

    List<? extends Volume> identifyUnmappedVolumes(long accountId, Set<Long> volumeIds);

    List<? extends Volume> findNonRemovedVolumesOnPool(Long storagePoolId);

    List<? extends Image> findBadImages(int count);

    List<? extends ImageStoragePoolMap> findBadImageStoragePoolMaps(int count);

    List<? extends Volume> findBadNativeVolumes(int count);

}
