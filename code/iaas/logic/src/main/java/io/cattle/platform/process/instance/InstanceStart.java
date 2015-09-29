package io.cattle.platform.process.instance;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.InstanceLinkConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.IpAddressDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.InstanceLink;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.util.InstanceHelpers;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.process.containerevent.ContainerEventCreate;
import io.cattle.platform.process.progress.ProcessProgress;
import io.cattle.platform.util.exception.ExecutionException;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicIntProperty;

@Named
public class InstanceStart extends AbstractDefaultProcessHandler {

    private static final DynamicIntProperty COMPUTE_TRIES = ArchaiusUtil.getInt("instance.compute.tries");
    private static final Logger log = LoggerFactory.getLogger(InstanceStart.class);

    GenericMapDao mapDao;
    IpAddressDao ipAddressDao;
    ProcessProgress progress;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        final Instance instance = (Instance) state.getResource();

        Map<String, Object> resultData = new ConcurrentHashMap<String, Object>();
        HandlerResult result = new HandlerResult(resultData);

        progress.init(state, 5, 5, 80, 5, 5);

        try {
            progress.checkPoint("Scheduling");
            allocate(instance);

            progress.checkPoint("Networking");
            network(instance, state);

            progress.checkPoint("Storage");
            storage(instance, state);
        } catch (ExecutionException e) {
            log.error("Failed to {} for instance [{}]", progress.getCurrentCheckpoint(), instance.getId());
            return stopOrRemove(state, instance, e);
        }

        try {
            progress.checkPoint("Starting");
            compute(instance, state);
        } catch (ExecutionException e) {
            log.error("Failed to {} for instance [{}]", progress.getCurrentCheckpoint(), instance.getId());
            if (incrementComputeTry(state) >= getMaxComputeTries(instance)) {
                return stopOrRemove(state, instance, e);
            }
            throw e;
        }

        try {
            progress.checkPoint("Post-network");
            postNetwork(instance, state);
        } catch (ExecutionException e) {
            log.error("Failed to {} for instance [{}]", progress.getCurrentCheckpoint(), instance.getId());
            return stopOrRemove(state, instance, e);
        }

        assignPrimaryIpAddress(instance, resultData);

        return result;
    }

    protected void assignPrimaryIpAddress(Instance instance, Map<String, Object> resultData) {
        int min = Integer.MAX_VALUE;
        IpAddress ip = null;
        IpAddress fallBackIp = null;
        for (Nic nic : getObjectManager().children(instance, Nic.class)) {
            if (nic.getDeviceNumber().intValue() < min) {
                min = nic.getDeviceNumber();
                ip = ipAddressDao.getPrimaryIpAddress(nic);
                if (ip == null) {
                    List<IpAddress> ips = getObjectManager().mappedChildren(nic, IpAddress.class);
                    if (ips.size() > 0) {
                        fallBackIp = ips.get(0);
                    }
                }
            }
        }

        String address = null;
        if (ip == null) {
            address = fallBackIp == null ? null : fallBackIp.getAddress();
        } else {
            address = ip.getAddress();
        }

        if (address != null) {
            resultData.put(InstanceConstants.FIELD_PRIMARY_IP_ADDRESS, address);
        }

        IpAddress assoc = ipAddressDao.getPrimaryAssociatedIpAddress(ip);
        if (assoc != null) {
            resultData.put(InstanceConstants.FIELD_PRIMARY_ASSOCIATED_IP_ADDRESS, assoc.getAddress());
        }
    }

    protected int getMaxComputeTries(Instance instance) {
        Integer tries = DataAccessor.fromDataFieldOf(instance).withScope(InstanceStart.class).withKey("computeTries").as(Integer.class);

        if (tries != null && tries > 0) {
            return tries;
        }

        return COMPUTE_TRIES.get();
    }

    protected HandlerResult stopOrRemove(ProcessState state, Instance instance, ExecutionException e) {
        if (InstanceCreate.isCreateStart(state) && !ContainerEventCreate.isNativeDockerStart(state) ) {
            getObjectProcessManager().scheduleProcessInstance(InstanceConstants.PROCESS_STOP, instance,
                    CollectionUtils.asMap(InstanceConstants.REMOVE_OPTION, true));
        } else {
            getObjectProcessManager().scheduleProcessInstance(InstanceConstants.PROCESS_STOP, instance, null);
        }

        e.setResources(state.getResource());
        throw e;
    }

    protected int incrementComputeTry(ProcessState state) {
        DataAccessor accessor = DataAccessor.fromMap(state.getData()).withScope(InstanceStart.class).withKey("computeTry");

        Integer computeTry = accessor.as(Integer.class);
        if (computeTry == null) {
            computeTry = 0;
        }

        computeTry++;

        accessor.set(computeTry);

        return computeTry;
    }

    protected void allocate(Instance instance) {
        execute("instance.allocate", instance, null);
    }

    protected void storage(Instance instance, ProcessState state) {
        List<Volume> volumes = getObjectManager().children(instance, Volume.class);

        volumes.addAll(InstanceHelpers.extractVolumesFromMounts(instance, objectManager));

        for (Volume volume : volumes) {
            activate(volume, state.getData());
        }
    }

    protected void compute(Instance instance, ProcessState state) {
        for (InstanceHostMap map : mapDao.findNonRemoved(InstanceHostMap.class, Instance.class, instance.getId())) {
            activate(map, state.getData());
        }
    }

    protected void network(Instance instance, ProcessState state) {
        for (Nic nic : getObjectManager().children(instance, Nic.class)) {
            activate(nic, state.getData());
        }

        for (InstanceLink link : getObjectManager().children(instance, InstanceLink.class, InstanceLinkConstants.FIELD_INSTANCE_ID)) {
            if (link.getRemoved() == null) {
                activate(link, state.getData());
            }
        }
    }

    protected void postNetwork(Instance instance, ProcessState state) {
        for (Port port : getObjectManager().children(instance, Port.class)) {
            // ports can be removed while instance is still present (lb instance is an example)
            if (port.getRemoved() == null
                    && !(port.getState().equalsIgnoreCase(CommonStatesConstants.REMOVED) || port.getState()
                            .equalsIgnoreCase(CommonStatesConstants.REMOVING))) {
                activate(port, state.getData());
            }
        }
    }

    public GenericMapDao getMapDao() {
        return mapDao;
    }

    @Inject
    public void setMapDao(GenericMapDao mapDao) {
        this.mapDao = mapDao;
    }

    public IpAddressDao getIpAddressDao() {
        return ipAddressDao;
    }

    @Inject
    public void setIpAddressDao(IpAddressDao ipAddressDao) {
        this.ipAddressDao = ipAddressDao;
    }

    public ProcessProgress getProgress() {
        return progress;
    }

    @Inject
    public void setProgress(ProcessProgress progress) {
        this.progress = progress;
    }

}
