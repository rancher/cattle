package io.cattle.platform.process.instance;

import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.InstanceLinkConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.dao.IpAddressDao;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.InstanceLink;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.process.containerevent.ContainerEventCreate;
import io.cattle.platform.process.progress.ProcessProgress;
import io.cattle.platform.util.exception.ExecutionException;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicIntProperty;

@Named
public class InstanceStart extends AbstractDefaultProcessHandler {

    private static final DynamicIntProperty COMPUTE_TRIES = ArchaiusUtil.getInt("instance.compute.tries");

    private static final List<String> REMOVED_STATES = Arrays.asList(CommonStatesConstants.REMOVED, CommonStatesConstants.REMOVING,
            CommonStatesConstants.PURGED, CommonStatesConstants.PURGING);

    private static final List<String> STOPPED_STATES = Arrays.asList(InstanceConstants.STATE_STOPPED, InstanceConstants.STATE_STOPPING);

    private static final List<String> START_ONCE_STATES = Arrays.asList(InstanceConstants.STATE_STOPPED, InstanceConstants.STATE_STOPPING,
            InstanceConstants.STATE_RUNNING);

    private static final List<String> UNALLOCATED_WAIT_STATES = Arrays.asList(CommonStatesConstants.REQUESTED, CommonStatesConstants.CREATING);

    private static final Logger log = LoggerFactory.getLogger(InstanceStart.class);

    @Inject
    JsonMapper jsonMapper;

    @Inject
    InstanceDao instanceDao;

    GenericMapDao mapDao;
    IpAddressDao ipAddressDao;
    ProcessProgress progress;

    @Inject
    ResourceMonitor resourceMonitor;

    @Inject
    ServiceDao serviceDao;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        final Instance instance = (Instance) state.getResource();

        Map<String, Object> resultData = new ConcurrentHashMap<String, Object>();
        HandlerResult result = new HandlerResult(resultData);

        progress.init(state, 16, 16, 16, 16, 20, 16);

        try {
            try {
                progress.checkPoint("Networking");
                network(instance, state);

                progress.checkPoint("Waiting for dependencies");
                // wait until volumesFrom/networksFrom containers start up
                waitForDependenciesStart(instance);

                ///wait until all containers in deployment unit are starting
                waitForDeploymentUnit(instance);

                progress.checkPoint("Scheduling");
                allocate(instance);
                activatePorts(instance, state);

                instanceDao.clearCacheInstanceData(instance.getId());

                progress.checkPoint("Storage");
                storage(instance, state);
            } catch (ExecutionException e) {
                log.error("Failed to {} for instance [{}]", progress.getCurrentCheckpoint(), instance.getId());
                return handleStartError(state, instance, e);
            }

            try {
                progress.checkPoint("Starting");
                compute(instance, state);
            } catch (ExecutionException e) {
                log.error("Failed to {} for instance [{}]", progress.getCurrentCheckpoint(), instance.getId());
                if (incrementComputeTry(state) >= getMaxComputeTries(instance)) {
                    return handleStartError(state, instance, e);
                }
                throw e;
            }
        } catch (TimeoutException e) {
            handleReconnecting(state, instance);
            throw e;
        }

        try {
            progress.checkPoint("Post-network");
            activatePorts(instance, state);
        } catch (ExecutionException e) {
            log.error("Failed to {} for instance [{}]", progress.getCurrentCheckpoint(), instance.getId());
            return handleStartError(state, instance, e);
        }

        assignPrimaryIpAddress(instance, resultData);

        instanceDao.clearCacheInstanceData(instance.getId());

        return result;
    }

    protected void waitForDeploymentUnit(Instance instance) {
        // Wait until all instances in the deployment unit are out of the creating state (to ensure all of instnace.create has ran)
        if(StringUtils.isEmpty(instance.getDeploymentUnitUuid())) {
            return;
        }

        List<? extends Instance> duInstances =
                instanceDao.findUnallocatedInstanceByDeploymentUnitUuid(instance.getAccountId(), instance.getDeploymentUnitUuid());

        List<Instance> waitList = new ArrayList<>();
        for (Instance i : duInstances) {
            if (UNALLOCATED_WAIT_STATES.contains(i.getState())) {
                waitList.add(i);
            }
        }

        //timeout is 30 seconds
        Long timeout =  30000L;
        for (Instance wait : waitList) {
            try {
                resourceMonitor.waitFor(wait, timeout,
                    new ResourcePredicate<Instance>() {
                        @Override
                        public boolean evaluate(Instance obj) {
                            return !UNALLOCATED_WAIT_STATES.contains(obj.getState());
                        }

                        @Override
                        public String getMessage() {
                            return "starting state";
                        }
                    }
                );
            } catch (TimeoutException e) {
                throw new ExecutionException("Dependencies readiness error", "instance hasn't goten passed creating state", instance.getId());
            }
        }
    }

    protected void waitForDependenciesStart(Instance instance) {
        List<Long> instancesIds = DataAccessor.fieldLongList(instance, DockerInstanceConstants.FIELD_VOLUMES_FROM);
        Long networkFromId = DataAccessor.fieldLong(instance, DockerInstanceConstants.FIELD_NETWORK_CONTAINER_ID);
        if (networkFromId != null) {
            instancesIds.add(networkFromId);
        }
        List<Instance> waitList = new ArrayList<>();
        for (Long id : instancesIds) {
            Instance i = objectManager.loadResource(Instance.class, id);

            String type = networkFromId != null && networkFromId.equals(id) ? "networkFrom" : "volumeFrom";
            if (REMOVED_STATES.contains(i.getState())) {
                throw new ExecutionException("Dependencies readiness error", type + " instance is removed", instance.getId());
            }

            if (!isStartOnce(i) && !serviceDao.isServiceInstance(instance) && STOPPED_STATES.contains(i.getState())) {
                throw new ExecutionException("Dependencies readiness error", type + " instance is not running",
                        instance.getId());
            }
            waitList.add(i);
        }

        //timeout is 2 mins
        Long timeout =  120000L;
        for (Instance wait : waitList) {
            try {
                resourceMonitor.waitFor(wait, timeout,
                    new ResourcePredicate<Instance>() {
                        @Override
                        public boolean evaluate(Instance obj) {
                            if (isStartOnce(obj)) {
                                return START_ONCE_STATES.contains(obj.getState());
                            }

                            if (obj.getRemoved() != null) {
                                throw new TimeoutException("Instance is removed");
                            }

                            InstanceHostMap ihm =
                                    objectManager.findAny(InstanceHostMap.class, INSTANCE_HOST_MAP.INSTANCE_ID, obj.getId(), INSTANCE_HOST_MAP.STATE,
                                            CommonStatesConstants.ACTIVE, INSTANCE_HOST_MAP.REMOVED, null);
                            return ihm != null;
                        }

                        @Override
                        public String getMessage() {
                            return "running state";
                        }
                    }
                );
            } catch (TimeoutException e) {
                throw new ExecutionException("Dependencies readiness error", "instance is not running", instance.getId());
            }
        }
    }

    protected boolean isStartOnce(Instance instance) {
        Map<String, Object> labels = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS);
        if (labels.get(SystemLabels.LABEL_SERVICE_CONTAINER_START_ONCE) != null) {
            return Boolean.valueOf(((String) labels
                    .get(SystemLabels.LABEL_SERVICE_CONTAINER_START_ONCE)));
        }
        return false;
    }

    protected void handleReconnecting(ProcessState state, Instance instance) {
        boolean reconnecting = false;
        InstanceHealthCheck healthCheck = DataAccessor.field(instance,
                InstanceConstants.FIELD_HEALTH_CHECK, jsonMapper, InstanceHealthCheck.class);

        for (InstanceHostMap map : mapDao.findNonRemoved(InstanceHostMap.class, Instance.class, instance.getId())) {
            Host host = objectManager.loadResource(Host.class, map.getHostId());
            Agent agent = host == null ? null : objectManager.loadResource(Agent.class, host.getAgentId());
            if (agent != null && (AgentConstants.STATE_RECONNECTING.equals(agent.getState()) ||
                    AgentConstants.STATE_DISCONNECTED.equals(agent.getState()))) {
                reconnecting = true;
            } else {
                reconnecting = false;
                break;
            }
        }

        if (reconnecting && (healthCheck != null || instance.getFirstRunning() == null)) {
            getObjectProcessManager().scheduleProcessInstance(InstanceConstants.PROCESS_STOP, instance,
                    CollectionUtils.asMap(InstanceConstants.REMOVE_OPTION, true));
        }
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
    }

    protected int getMaxComputeTries(Instance instance) {
        Integer tries = DataAccessor.fromDataFieldOf(instance).withScope(InstanceStart.class).withKey("computeTries").as(Integer.class);

        if (tries != null && tries > 0) {
            return tries;
        }

        return COMPUTE_TRIES.get();
    }

    protected HandlerResult handleStartError(ProcessState state, Instance instance, ExecutionException e) {
        if (InstanceCreate.isCreateStart(state) && !ContainerEventCreate.isNativeDockerStart(state) ) {
            HashMap<String, Object> data = new HashMap<String, Object>();
            data.put(InstanceConstants.PROCESS_DATA_ERROR, true);
            getObjectProcessManager().scheduleProcessInstance(InstanceConstants.PROCESS_STOP, instance,
                    ProcessUtils.chainInData(new HashMap<String, Object>(), InstanceConstants.PROCESS_STOP,
                            InstanceConstants.PROCESS_ERROR));
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

    protected void activatePorts(Instance instance, ProcessState state) {
        for (Port port : getObjectManager().children(instance, Port.class)) {
            // ports can be removed while instance is still present (lb instance is an example)
            if (port.getRemoved() == null
                    && !(port.getState().equalsIgnoreCase(CommonStatesConstants.REMOVED) || port.getState()
                            .equalsIgnoreCase(CommonStatesConstants.REMOVING))) {
                createThenActivate(port, state.getData());
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
