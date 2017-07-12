package io.cattle.platform.trigger;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.dao.VolumeDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.engine.manager.LoopManager;
import io.cattle.platform.engine.model.Trigger;
import io.cattle.platform.loop.factory.LoopFactoryImpl;
import io.cattle.platform.object.ObjectManager;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static io.cattle.platform.core.model.tables.HostTable.*;

public class DeploymentUnitReconcileTrigger implements Trigger {

    LoopManager loopManager;
    ServiceDao serviceDao;
    VolumeDao volumeDao;
    ObjectManager objectManager;

    public DeploymentUnitReconcileTrigger(LoopManager loopManager, ServiceDao serviceDao, VolumeDao volumeDao, ObjectManager objectManager) {
        super();
        this.loopManager = loopManager;
        this.serviceDao = serviceDao;
        this.volumeDao = volumeDao;
        this.objectManager = objectManager;
    }

    @Override
    public void trigger(Long accountId, Object resource, String source) {
        for (Long id : getDeploymentUnits(resource)) {
            loopManager.kick(LoopFactoryImpl.DU_RECONCILE, ServiceConstants.KIND_DEPLOYMENT_UNIT, id, resource);
        }
    }

    private Collection<Long> getDeploymentUnits(Object obj) {
        Host host = null;
        if (obj instanceof Host) {
            host = (Host) obj;
        }
        if (obj instanceof Agent) {
            Agent agent = (Agent) obj;
            host = objectManager.findAny(Host.class, HOST.AGENT_ID, agent.getId());
        }
        if (host != null) {
            return serviceDao.getServiceDeploymentUnitsOnHost(host);
        }
        if (obj instanceof DeploymentUnit) {
            return Arrays.asList(((DeploymentUnit) obj).getId());
        }
        if (obj instanceof Instance) {
            return Arrays.asList(((Instance) obj).getDeploymentUnitId());
        }
        if (obj instanceof Volume) {
            Volume vol = (Volume) obj;
            if (vol.getDeploymentUnitId() == null && vol.getVolumeTemplateId() != null) {
                return volumeDao.findDeploymentUnitsForVolume(vol);
            }
            if (vol.getDeploymentUnitId() != null) {
                return Arrays.asList(vol.getDeploymentUnitId());
            }
        }

        return Collections.emptyList();
    }

}