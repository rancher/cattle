package io.cattle.platform.iaas.api.volume;

import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.VolumeDao;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

public class VolumeRevertRestoreActionOutputFilter implements ResourceOutputFilter {

    @Inject
    VolumeDao volumeDao;

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        if (!(original instanceof Volume)) {
            return converted;
        }

        Volume volume = (Volume)original;

        List<String> caps = DataAccessor.fieldStringList(volume, ObjectMetaDataManager.CAPABILITIES_FIELD);
        Set<String> capabilities = new HashSet<>(caps);
        boolean snapshotCapable = capabilities.contains(VolumeConstants.CAPABILITY_SNAPSHOT);
        if (!snapshotCapable) {
            converted.getActions().remove(VolumeConstants.ACTION_SNAPSHOT);
        }

        if (!snapshotCapable || volumeDao.isVolumeInUseByRunningInstance(volume.getId())) {
            converted.getActions().remove(VolumeConstants.ACTION_REVERT);
            converted.getActions().remove(VolumeConstants.ACTION_RESTORE);
        }

        return converted;
    }

    @Override
    public String[] getTypes() {
        return new String[] { "volume" };
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Volume.class };
    }

}
