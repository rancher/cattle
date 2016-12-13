package io.cattle.platform.process.host;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.HostIpAddressMap;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.PhysicalHost;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.StoragePoolHostMap;
import io.cattle.platform.docker.constants.DockerHostConstants;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.util.type.CollectionUtils;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class HostRemove extends AbstractDefaultProcessHandler {

    @Inject
    InstanceDao instanceDao;

    @Override
    public HandlerResult handle(final ProcessState state, ProcessInstance process) {
        final Host host = (Host) state.getResource();

        removeInstances(host);
        removeIps(host);
        removePools(host);

        PhysicalHost physHost = objectManager.loadResource(PhysicalHost.class, host.getPhysicalHostId());
        if (physHost != null) {
            deactivateThenScheduleRemove(physHost, null);
        }

        return null;
    }

    protected void removePools(Host host) {
        for (StoragePoolHostMap map : objectManager.children(host, StoragePoolHostMap.class)) {
            StoragePool pool = objectManager.loadResource(StoragePool.class, map.getStoragePoolId());
            if (DockerHostConstants.KIND_DOCKER.equals(pool.getKind())) {
                deactivateThenRemove(pool, null);
            }
            deactivateThenRemove(map, null);
        }
    }

    protected void removeIps(Host host) {
        for (HostIpAddressMap map : objectManager.children(host, HostIpAddressMap.class)) {
            IpAddress ipAddress = objectManager.loadResource(IpAddress.class, map.getIpAddressId());
            deactivateThenRemove(ipAddress, null);
        }
    }

    protected void removeInstances(Host host) {
        for (Instance instance : instanceDao.getNonRemovedInstanceOn(host.getId())) {
            try {
                objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, instance, null);
            } catch (ProcessCancelException e) {
                objectProcessManager.scheduleProcessInstance(InstanceConstants.PROCESS_STOP, instance,
                        CollectionUtils.asMap(InstanceConstants.REMOVE_OPTION, true));
            }
        }
    }

}
