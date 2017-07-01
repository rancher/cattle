package io.cattle.platform.containersync.impl;

import static io.cattle.platform.core.constants.CommonStatesConstants.*;
import static io.cattle.platform.core.constants.ContainerEventConstants.*;
import static io.cattle.platform.core.constants.InstanceConstants.*;
import static io.cattle.platform.core.model.tables.HostTable.*;

import io.cattle.platform.containersync.ContainerSync;
import io.cattle.platform.containersync.model.ContainerEventEvent;
import io.cattle.platform.core.addon.ContainerEvent;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.engine.server.Cluster;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.containerevent.ContainerEventInstanceLock;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.cloudstack.managed.context.NoExceptionRunnable;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContainerSyncImpl implements ContainerSync {

    private static final String INSPECT_LABELS = "Labels";
    private static final String INSPECT_NAME = "Name";
    private static final String INSPECT_CONFIG = "Config";

    private static final Logger log = LoggerFactory.getLogger(ContainerSyncImpl.class);

    ObjectManager objectManager;
    ObjectProcessManager processManager;
    InstanceDao instanceDao;
    LockManager lockManager;
    GenericResourceDao resourceDao;
    ScheduledExecutorService scheduledExecutorService;
    Cluster cluster;

    @Override
    public void containerEvent(ContainerEventEvent eventEvent) {
        if (cluster.isInPartition(Long.parseLong(eventEvent.getResourceId()))) {
            containerEvent(eventEvent, 0);
        }
    }

    private void containerEvent(ContainerEventEvent eventEvent, int retry) {
        ContainerEvent event = eventEvent.getData();

        Host host = objectManager.findOne(Host.class, HOST.ID, event.getHostId());
        if (host == null || host.getRemoved() != null
                || host.getState().equalsIgnoreCase(CommonStatesConstants.REMOVING)) {
            log.info("Host [{}] is unavailable. Not processing container event", event.getHostId());
            return;
        }

        lockManager.lock(new ContainerEventInstanceLock(event.getAccountId(), event.getExternalId()), () -> {
            try {
                containerEventWithLock(event);
            } catch (ProcessCancelException e) {
            } catch (RetryLater e) {
                if (retry < 3) {
                    scheduledExecutorService.schedule(new NoExceptionRunnable() {
                        @Override
                        protected void doRun() throws Exception {
                            containerEvent(eventEvent, retry+1);
                        }
                    }, 3, TimeUnit.SECONDS);
                }
            }
            return null;
        });
    }

    private void containerEventWithLock(ContainerEvent event) {
        Instance instance = instanceDao.getInstanceByUuidOrExternalId(event.getAccountId(), event.getUuid(), event.getExternalId());

        String status = event.getExternalStatus();
        if (status.equals(EVENT_START) && instance == null && event.getDockerInspect() != null) {
            scheduleInstance(event);
            return;
        }

        if (instance == null || instance.getRemoved() != null) {
            return;
        }

        sync(event.getExternalStatus(), instance);
    }

    private void sync(String status, Instance instance) {
        String state = instance.getState();

        if (EVENT_START.equals(status)) {
            if (STATE_CREATING.equals(state) || STATE_RUNNING.equals(state) || STATE_STARTING.equals(state) || STATE_RESTARTING.equals(status))
                return;

            if (STATE_STOPPING.equals(state)) {
                // handle docker restarts
                throw new RetryLater();
            }

            processManager.scheduleProcessInstance(PROCESS_START, instance, makeData());
        } else if (EVENT_STOP.equals(status) || EVENT_DIE.equals(status)) {
            if (STATE_STOPPED.equals(state) || STATE_STOPPING.equals(state))
                return;

            if (STATE_STARTING.equals(state)) {
                throw new RetryLater();
            }

            processManager.scheduleProcessInstance(PROCESS_STOP, objectManager.reload(instance), makeData());
        } else if (EVENT_DESTROY.equals(status)) {
            if (REMOVED.equals(state) || REMOVING.equals(state))
                return;

            Map<String, Object> processData = makeData();
            try {
                processManager.scheduleStandardProcess(StandardProcess.REMOVE, instance, processData);
            } catch (ProcessCancelException e) {
                if (STATE_STOPPING.equals(state)) {
                    // handle docker forced stop and remove
                    throw new RetryLater();
                } else {
                    processManager.stopThenRemove(instance, processData);
                }
            }
        }
    }

    private void scheduleInstance(ContainerEvent event) {
        Map<String, Object> inspect = event.getDockerInspect();
        final Long accountId = event.getAccountId();
        final String externalId = event.getExternalId();

        Instance instance = objectManager.newRecord(Instance.class);
        instance.setKind(KIND_CONTAINER);
        instance.setAccountId(accountId);
        instance.setExternalId(externalId);
        instance.setNativeContainer(true);
        setName(inspect, event, instance);
        setNetwork(inspect, event, instance);
        setImage(event, instance);
        setHost(event, instance);
        setLabels(event, instance);

        resourceDao.createAndSchedule(instance, makeData());
    }

    private void setLabels(ContainerEvent event, Instance instance) {
        Map<String, Object> labels = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS);
        if ("true".equals(labels.get(SystemLabels.LABEL_RANCHER_NETWORK))) {
            labels.put(SystemLabels.LABEL_CNI_NETWORK, NetworkConstants.NETWORK_MODE_MANAGED);
        }
        labels.putAll(getLabels(event));
        DataAccessor.setField(instance, InstanceConstants.FIELD_LABELS, labels);
    }

    protected Map<String, Object> makeData() {
        Map<String, Object> data = new HashMap<>();
        DataAccessor.fromMap(data).withKey(PROCESS_DATA_NO_OP).set(true);
        return data;
    }

    private void setHost(ContainerEvent event, Instance instance) {
        DataAccessor.fields(instance).withKey(FIELD_REQUESTED_HOST_ID).set(event.getHostId());
    }

    private void setName(Map<String, Object> inspect, ContainerEvent event, Instance instance) {
        String name = DataAccessor.fromMap(inspect).withKey(INSPECT_NAME).as(String.class);
        if (name != null)
            name = name.replaceFirst("/", "");
        instance.setName(name);
    }

    private void setNetwork(Map<String, Object> inspect, ContainerEvent event, Instance instance) {
        String inspectNetMode = getInspectNetworkMode(inspect);
        String networkMode = checkNoneNetwork(inspect);

        if (networkMode == null) {
            networkMode = checkBlankNetwork(inspect);
        }

        if (networkMode == null) {
            networkMode = checkContainerNetwork(inspectNetMode, instance);
        }

        if (networkMode == null) {
            networkMode = inspectNetMode;
        }

        DataAccessor.fields(instance).withKey(FIELD_NETWORK_MODE).set(networkMode);

        String ip = getDockerIp(inspect);
        if (StringUtils.isNotEmpty(ip)) {
            DataAccessor.fields(instance).withKey(FIELD_REQUESTED_IP_ADDRESS).set(ip);
        }
    }

    private String getInspectNetworkMode(Map<String, Object> inspect) {
        Object tempNM = CollectionUtils.getNestedValue(inspect, "HostConfig", "NetworkMode");
        String inspectNetMode = tempNM == null ? "" : tempNM.toString();
        return inspectNetMode;
    }

    private String checkNoneNetwork(Map<String, Object> inspect) {
        Object netDisabledObj = CollectionUtils.getNestedValue(inspect, "Config", "NetworkDisabled");
        if (Boolean.TRUE.equals(netDisabledObj)) {
            return NetworkConstants.NETWORK_MODE_NONE;
        }
        return null;
    }

    private String checkBlankNetwork(Map<String, Object> inspect) {
        String inspectNm = getInspectNetworkMode(inspect);
        if (StringUtils.isBlank(inspectNm)) {
            return NetworkConstants.NETWORK_MODE_BRIDGE;
        }
        return null;
    }

    /*
     * If mode is container, will attempt to look up the corresponding container in rancher. If it is found, will also have the side effect of setting
     * networkContainerId on the instance.
     */
    private String checkContainerNetwork(String inspectNetMode, Instance instance) {
        if (!StringUtils.startsWith(inspectNetMode, NetworkConstants.NETWORK_MODE_CONTAINER))
            return null;

        String[] parts = StringUtils.split(inspectNetMode, ":", 2);
        String targetContainer = null;
        if (parts.length == 2) {
            targetContainer = parts[1];
            Instance netFromInstance = instanceDao.getInstanceByUuidOrExternalId(instance.getAccountId(), targetContainer, targetContainer);
            if (netFromInstance == null) {
                throw new RetryLater();
            }

            if (netFromInstance != null) {
                DataAccessor.fields(instance).withKey(FIELD_NETWORK_CONTAINER_ID).set(netFromInstance.getId());
                return NetworkConstants.NETWORK_MODE_CONTAINER;
            }
        }

        log.warn("Problem configuring container networking for container [externalId: {}]. Could not find target container: [{}].", instance.getExternalId(),
                targetContainer);

        return NetworkConstants.NETWORK_MODE_NONE;
    }

    private String getDockerIp(Map<String, Object> inspect) {
        Object ip = CollectionUtils.getNestedValue(inspect, "NetworkSettings", "IPAddress");
        if (ip != null)
            return ip.toString();

        return null;
    }

    private void setImage(ContainerEvent event, Instance instance) {
        Object imageUuid = CollectionUtils.getNestedValue(event.getDockerInspect(), INSPECT_CONFIG, "Image");
        DataAccessor.fields(instance).withKey(FIELD_IMAGE_UUID).set(imageUuid);
    }

    private Map<String, Object> getLabels(ContainerEvent event) {
        return CollectionUtils.toMap(CollectionUtils.getNestedValue(event.getDockerInspect(), INSPECT_CONFIG, INSPECT_LABELS));
    }

    private static class RetryLater extends RuntimeException {
        private static final long serialVersionUID = 7123284316672856140L;
    }
}
