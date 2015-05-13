package io.cattle.platform.process.containerevent;

import static io.cattle.platform.core.constants.CommonStatesConstants.*;
import static io.cattle.platform.core.constants.ContainerEventConstants.*;
import static io.cattle.platform.core.constants.InstanceConstants.*;
import static io.cattle.platform.core.constants.NetworkConstants.*;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.ContainerEvent;
import io.cattle.platform.core.model.Image;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.storage.service.StorageService;
import io.cattle.platform.util.exception.ExecutionException;
import io.cattle.platform.util.net.NetUtils;
import io.cattle.platform.util.type.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.netflix.config.DynamicBooleanProperty;

@Named
public class ContainerEventCreate extends AbstractDefaultProcessHandler {

    private static final DynamicBooleanProperty MANAGE_NONRANCHER_CONTAINERS = ArchaiusUtil.getBoolean("manage.nonrancher.containers");
    private static final String INSPECT_ENV = "Env";
    private static final String INSPECT_LABELS = "Labels";
    private static final String INSPECT_NAME = "Name";
    private static final String FIELD_DOCKER_INSPECT = "dockerInspect";
    private static final String INSPECT_CONFIG = "Config";
    private static final String IMAGE_PREFIX = "docker:";
    private static final String IMAGE_KIND_PATTERN = "^(sim|docker):.*";
    private static final String RANCHER_UUID_ENV_VAR = "RANCHER_UUID=";
    private static final String RANCHER_NETWORK_ENV_VAR = "RANCHER_NETWORK=";

    @Inject
    StorageService storageService;

    @Inject
    NetworkDao networkDao;

    @Inject
    AccountDao accountDao;

    @Inject
    InstanceDao instanceDao;

    @Inject
    LockManager lockManager;

    @Inject
    ResourceMonitor resourceMonitor;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        if (!MANAGE_NONRANCHER_CONTAINERS.get()) {
            return null;
        }

        final ContainerEvent event = (ContainerEvent)state.getResource();
        final Map<String, Object> data = state.getData();
        HandlerResult result = lockManager.lock(new ContainerEventInstanceLock(event.getAccountId(), event.getExternalId()), new LockCallback<HandlerResult>() {
            @Override
            public HandlerResult doWithLock() {
                Map<String, Object> inspect = getInspect(event);
                String rancherUuid = getRancherUuidLabel(inspect, data);
                Instance instance = instanceDao.getInstanceByUuidOrExternalId(event.getAccountId(), rancherUuid, event.getExternalId());
                if ((instance != null && StringUtils.isNotEmpty(instance.getSystemContainer()))
                                || StringUtils.isNotEmpty(getLabel(LABEL_RANCHER_SYSTEM_CONTAINER, inspect, data))) {
                    // System containers are not managed by container events
                    return null;
                }

                try {
                    String status = event.getExternalStatus();
                    if (status.equals(EVENT_CREATE) && instance == null) {
                        scheduleInstance(event, instance, inspect, data);
                        return null;
                    }

                    if (instance == null) {
                        return null;
                    }

                    String state = instance.getState();
                    if (EVENT_START.equals(status)) {
                        if (STATE_CREATING.equals(state) || STATE_RUNNING.equals(state) || STATE_STARTING.equals(state) || STATE_RESTARTING.equals(status))
                            return null;

                        if (STATE_STOPPING.equals(state)) {
                            // handle docker restarts
                            instance = resourceMonitor.waitForNotTransitioning(instance);
                        }

                        objectProcessManager.scheduleProcessInstance(PROCESS_START, instance, makeData());
                    } else if (EVENT_STOP.equals(status) || EVENT_DIE.equals(status)) {
                        if (STATE_STOPPED.equals(state) || STATE_STOPPING.equals(state))
                            return null;

                        objectProcessManager.scheduleProcessInstance(PROCESS_STOP, instance, makeData());
                    } else if (EVENT_DESTROY.equals(status)) {
                        if (REMOVED.equals(state) || REMOVING.equals(state) || PURGED.equals(state) || PURGING.equals(state))
                            return null;

                        Map<String, Object> data = makeData();
                        try {
                            objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, instance, data);
                        } catch (ProcessCancelException e) {
                            data.put(REMOVE_OPTION, true);
                            objectProcessManager.scheduleProcessInstance(PROCESS_STOP, instance, data);
                        }
                    }
                } catch (ProcessCancelException e) {
                    // ignore
                }
                return null;
            }
        });

        return result;
    }

    void scheduleInstance(ContainerEvent event, Instance instance, Map<String, Object> inspect, Map<String, Object> data) {
        final Long accountId = event.getAccountId();
        final String externalId = event.getExternalId();

        instance = objectManager.newRecord(Instance.class);
        instance.setKind(KIND_CONTAINER);
        instance.setAccountId(accountId);
        instance.setExternalId(externalId);
        instance.setNativeContainer(true);
        setName(inspect, data, instance);
        setNetwork(inspect, data, instance);
        setImage(event, instance);
        setHost(event, instance);
        instance = objectManager.create(instance);
        objectProcessManager.scheduleProcessInstance(PROCESS_CREATE, instance, makeData());
    }

    Map<String, Object> getInspect(ContainerEvent event) {
        Object obj = DataUtils.getFields(event).get(FIELD_DOCKER_INSPECT);
        if (obj == null)
            return null;
        return CollectionUtils.toMap(obj);
    }

    public static boolean isNativeDockerStart(ProcessState state) {
        return DataAccessor.fromMap(state.getData()).withScope(ContainerEventCreate.class).withKey(PROCESS_DATA_NO_OP).withDefault(false).as(Boolean.class);
    }

    protected Map<String, Object> makeData() {
        Map<String, Object> data = new HashMap<String, Object>();
        DataAccessor.fromMap(data).withKey(PROCESS_DATA_NO_OP).set(true);
        return data;
    }

    void setHost(ContainerEvent event, Instance instance) {
        DataAccessor.fields(instance).withKey(FIELD_REQUESTED_HOST_ID).set(event.getHostId());
    }

    void setName(Map<String, Object> inspect, Map<String, Object> data, Instance instance) {
        String name = DataAccessor.fromMap(data).withKey(CONTAINER_EVENT_SYNC_NAME).as(String.class);
        if (StringUtils.isEmpty(name))
            name = (String)inspect.get(INSPECT_NAME);

        name = name.replaceFirst("/", "");
        instance.setName(name);
    }

    void setNetwork(Map<String, Object> inspect, Map<String, Object> data, Instance instance) {
        Network network = null;
        Network rancherNetwork = networkDao.getNetworkForObject(instance, KIND_HOSTONLY);
        String ip = getDockerIp(inspect);
        if (StringUtils.isNotEmpty(ip) && isIpInNetwork(ip, rancherNetwork)) {
            DataAccessor.fields(instance).withKey(FIELD_REQUESTED_IP_ADDRESS).set(ip);
            network = rancherNetwork;
        }

        if (network == null && BooleanUtils.toBoolean(getRancherNetworkLabel(inspect, data))) {
            network = rancherNetwork;
        }

        if (network == null) {
            network = networkDao.getNetworkForObject(instance, KIND_NETWORK);
        }

        if (network != null) {
            List<Long> netIds = new ArrayList<Long>();
            netIds.add(network.getId());
            DataAccessor.fields(instance).withKey(FIELD_NETWORK_IDS).set(netIds);
        }
    }

    boolean isIpInNetwork(String ipAddress, Network network) {
        for (Subnet subnet : objectManager.children(network, Subnet.class)) {
            if (subnet.getRemoved() == null) {
                String startIp = NetUtils.getDefaultStartAddress(subnet.getNetworkAddress(), subnet.getCidrSize());
                long start = NetUtils.ip2Long(startIp);
                String endIp = NetUtils.getDefaultEndAddress(subnet.getNetworkAddress(), subnet.getCidrSize());
                long end = NetUtils.ip2Long(endIp);
                long ip = NetUtils.ip2Long(ipAddress);
                if (start <= ip && ip <= end)
                    return true;
            }
        }
        return false;
    }

    String getDockerIp(Map<String, Object> inspect) {
        if (inspect == null)
            return null;

        Object ip = CollectionUtils.getNestedValue(inspect, "NetworkSettings", "IPAddress");
        if (ip != null)
            return ip.toString();

        return null;
    }

    void setImage(ContainerEvent event, Instance instance) {
        String imageUuid = event.getExternalFrom();

        // Somewhat of a hack, but needed for testing against sim contexts
        if (!imageUuid.matches(IMAGE_KIND_PATTERN)) {
            imageUuid = IMAGE_PREFIX + imageUuid;
        }
        DataAccessor.fields(instance).withKey(FIELD_IMAGE_UUID).set(imageUuid);
        Image image = null;
        try {
            image = storageService.registerRemoteImage(imageUuid);
        } catch (IOException e) {
            throw new ExecutionException("Unable to create image.", e);
        }
        instance.setImageId(image.getId());
    }

    String getRancherUuidLabel(Map<String, Object> inspect, Map<String, Object> data) {
        return getLabel(LABEL_RANCHER_UUID, RANCHER_UUID_ENV_VAR, inspect, data);
    }

    String getRancherNetworkLabel(Map<String, Object> inspect, Map<String, Object> data) {
        return getLabel(RANCHER_NETWORK, RANCHER_NETWORK_ENV_VAR, inspect, data);
    }

    String getLabel(String labelKey, Map<String, Object> inspect, Map<String, Object> data) {
        return getLabel(labelKey, null, inspect, data);
    }

    @SuppressWarnings("unchecked")
    String getLabel(String labelKey, String envVarPrefix, Map<String, Object> inspect, Map<String, Object> data) {
        Map<String, Object> labelsFromData = CollectionUtils.toMap(DataAccessor.fromMap(data).withKey(CONTAINER_EVENT_SYNC_LABELS).get());
        String label = labelsFromData.containsKey(labelKey) ? labelsFromData.get(labelKey).toString() : null;
        if (StringUtils.isNotEmpty(label))
            return label;

        if (inspect == null)
            return null;

        Map<String, Object> config = (Map<String, Object>)inspect.get(INSPECT_CONFIG);

        Map<String, String> labels = CollectionUtils.toMap(config.get(INSPECT_LABELS));
        label = labels.get(labelKey);
        if (StringUtils.isNotEmpty(label))
            return label;

        if (envVarPrefix == null)
            return null;

        List<String> envVars = (List<String>)CollectionUtils.toList(config.get(INSPECT_ENV));
        for (String envVar : envVars) {
            if (envVar.startsWith(envVarPrefix))
                return envVar.substring(envVarPrefix.length());
        }
        return null;
    }
}
