package io.cattle.platform.servicediscovery.deployment.lookups;

import io.cattle.platform.core.dao.VolumeDao;
import io.cattle.platform.core.model.Volume;

import java.util.Arrays;
import java.util.Collection;

import javax.inject.Inject;

public class VolumeDeploymentUnitLookup implements DeploymentUnitLookup {

    @Inject
    VolumeDao volumeDao;

    @Override
    public Collection<Long> getDeploymentUnits(Object obj) {
        if (obj instanceof Volume) {
            Volume vol = (Volume) obj;
            if (vol.getDeploymentUnitId() == null && vol.getVolumeTemplateId() != null) {
                return volumeDao.findDeploymentUnitsForVolume(vol);
            }
            if (vol.getDeploymentUnitId() != null) {
                return Arrays.asList(vol.getDeploymentUnitId());
            }
        }
        return null;
    }
}
