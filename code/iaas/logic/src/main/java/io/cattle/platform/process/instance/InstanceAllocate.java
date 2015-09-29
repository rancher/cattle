package io.cattle.platform.process.instance;

import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import io.cattle.platform.core.constants.ClusterConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.NicDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeStoragePoolMap;
import io.cattle.platform.core.util.InstanceHelpers;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.EventBasedProcessHandler;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class InstanceAllocate extends EventBasedProcessHandler {
    private static final Logger log = LoggerFactory.getLogger(InstanceAllocate.class);

    @Inject
    EventService eventService;

    @Inject
    NicDao nicDao;

    @Inject
    GenericMapDao mapDao;

    public InstanceAllocate() {
        setPriority(DEFAULT);
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        // Check resourceType for instance and whether the host is a cluster or not.
        // If resourceType is a cluster (host), do not bother with normal allocation process.
        final Instance instance = (Instance)state.getResource();

        // check requestedHostId to see whether it's a cluster or not
        Long requestedHostId = DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_REQUESTED_HOST_ID).as(Long.class);
        if (requestedHostId != null) {
            Host potentialCluster = objectManager.loadResource(Host.class, requestedHostId);
            if (objectManager.isKind(potentialCluster, ClusterConstants.KIND)) {
                handleCluster(requestedHostId, instance, potentialCluster);
                return postEvent(state, process, new HashMap<Object, Object>());
            }
        }
        if (mapDao.findNonRemoved(InstanceHostMap.class, Instance.class, instance.getId()).size() > 0) {
            return postEvent(state, process, new HashMap<Object, Object>());
        }
        return super.handle(state, process);
    }

    private void handleCluster(Long requestedHostId, Instance instance, Host cluster) {
        log.info("Instance [{}] requested for a cluster: [{}]", instance, cluster);

        // create instanceHostMap binding with cluster.
        if (mapDao.findNonRemoved(InstanceHostMap.class, Host.class, requestedHostId, Instance.class, instance.getId()) == null) {
            objectManager.create(InstanceHostMap.class,
                    INSTANCE_HOST_MAP.HOST_ID, requestedHostId,
                    INSTANCE_HOST_MAP.INSTANCE_ID, instance.getId());
        }

        // update nic with active subnet
        Nic nic = nicDao.getPrimaryNic(instance);
        Network network = objectManager.loadResource(Network.class, nic.getNetworkId());
        if (network != null) {
            for (Subnet subnet : objectManager.children(network, Subnet.class)) {
                if (CommonStatesConstants.ACTIVE.equals(subnet.getState())) {
                    nic.setSubnetId(subnet.getId());
                    objectManager.persist(nic);
                    break;
                }
            }
        }
    }

    @Override
    protected HandlerResult postEvent(ProcessState state, ProcessInstance process, Map<Object, Object> result) {
        Map<String, Set<Long>> allocationData = new HashMap<String, Set<Long>>();
        result.put("_allocationData", allocationData);

        Instance instance = (Instance) state.getResource();

        for (InstanceHostMap map : mapDao.findNonRemoved(InstanceHostMap.class, Instance.class, instance.getId())) {
            CollectionUtils.addToMap(allocationData, "instance:" + instance.getId(), map.getHostId(), HashSet.class);
            create(map, state.getData());
        }

        List<Volume> volumes = getObjectManager().children(instance, Volume.class);
        List<Volume> dataMountVolumes = InstanceHelpers.extractVolumesFromMounts(instance, getObjectManager());
        volumes.addAll(dataMountVolumes);

        for (Volume v : volumes) {
            allocate(v, state.getData());
        }

        volumes = getObjectManager().children(instance, Volume.class);
        volumes.addAll(dataMountVolumes);
        
        for (Volume v : volumes) {
            for (VolumeStoragePoolMap map : mapDao.findNonRemoved(VolumeStoragePoolMap.class, Volume.class, v.getId())) {
                CollectionUtils.addToMap(allocationData, "volume:" + v.getId(), map.getVolumeId(), HashSet.class);
                create(map, state.getData());
            }
        }

        return new HandlerResult(result);
    }

}
