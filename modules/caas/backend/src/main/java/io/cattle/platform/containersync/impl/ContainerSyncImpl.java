package io.cattle.platform.containersync.impl;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.containersync.ContainerSync;
import io.cattle.platform.containersync.model.ContainerEventEvent;
import io.cattle.platform.core.addon.ContainerEvent;
import io.cattle.platform.core.addon.DeploymentSyncRequest;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.dao.ClusterDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.engine.server.Cluster;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.lifecycle.InstanceLifecycleManager;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.serialization.ObjectSerializer;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.containerevent.ContainerEventInstanceLock;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.util.DateUtils;
import org.apache.cloudstack.managed.context.NoExceptionRunnable;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.cattle.platform.core.constants.CommonStatesConstants.*;
import static io.cattle.platform.core.constants.ContainerEventConstants.*;
import static io.cattle.platform.core.constants.InstanceConstants.*;
import static io.cattle.platform.core.model.Tables.*;
import static io.cattle.platform.core.model.tables.HostTable.HOST;

public class ContainerSyncImpl implements ContainerSync {

    private static final String INSPECT_LABELS = "Labels";
    private static final String INSPECT_NAME = "Name";
    private static final String INSPECT_CONFIG = "Config";

    private static final Logger log = LoggerFactory.getLogger(ContainerSyncImpl.class);
    private static Pattern NAMESPACE = Pattern.compile("^.*-([0-9a-f]{7})$");
    private static Set<String> HIDDEN_NAMES = CollectionUtils.set("dns-init", "rancher-pause", "POD");

    ObjectManager objectManager;
    ObjectProcessManager processManager;
    InstanceDao instanceDao;
    LockManager lockManager;
    GenericResourceDao resourceDao;
    ScheduledExecutorService scheduledExecutorService;
    Cluster cluster;
    ClusterDao clusterDao;
    InstanceLifecycleManager instanceLifecycleManager;
    AgentLocator agentLocator;
    ObjectSerializer objectSerializer;

    public ContainerSyncImpl(ObjectManager objectManager, ObjectProcessManager processManager, InstanceDao instanceDao, LockManager lockManager,
                             GenericResourceDao resourceDao, ScheduledExecutorService scheduledExecutorService, Cluster cluster, ClusterDao clusterDao,
                             InstanceLifecycleManager instanceLifecycleManager, AgentLocator agentLocator, ObjectSerializer objectSerializer) {
        this.objectManager = objectManager;
        this.processManager = processManager;
        this.instanceDao = instanceDao;
        this.lockManager = lockManager;
        this.resourceDao = resourceDao;
        this.scheduledExecutorService = scheduledExecutorService;
        this.cluster = cluster;
        this.clusterDao = clusterDao;
        this.instanceLifecycleManager = instanceLifecycleManager;
        this.agentLocator = agentLocator;
        this.objectSerializer = objectSerializer;
    }

    @Override
    public void containerEvent(ContainerEventEvent eventEvent) {
        if (cluster.isInPartition(null, eventEvent.getData().getClusterId())) {
            containerEvent(eventEvent, 0);
        }
    }

    private void containerEvent(ContainerEventEvent eventEvent, int retry) {
        ContainerEvent event = eventEvent.getData();

        Host host = objectManager.findAny(Host.class, HOST.ID, event.getHostId());
        if (host == null || host.getRemoved() != null
                || host.getState().equalsIgnoreCase(CommonStatesConstants.REMOVING)) {
            log.info("Host [{}] is unavailable. Not processing container event", event.getHostId());
            return;
        }

        lockManager.lock(new ContainerEventInstanceLock(event.getClusterId(), event.getExternalId()), () -> {
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
                } else {
                    log.warn("Giving up trying to process container event for clusterId [{}] hostUuid [{}] externalId [{}]", event.getClusterId(),
                            event.getReportedHostUuid(), event.getExternalId());
                }
            }
            return null;
        });
    }

    private void containerEventWithLock(ContainerEvent event) {
        Instance instance = findInstance(event.getClusterId(), event.getExternalId());
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

    private Instance findInstance(Long clusterId, String externalId) {
        return objectManager.findAny(Instance.class,
                INSTANCE.CLUSTER_ID, clusterId,
                INSTANCE.EXTERNAL_ID, externalId,
                INSTANCE.REMOVED, null);
    }

    private void sync(String status, Instance instance) {
        String state = instance.getState();

        if (EVENT_START.equals(status)) {
            if (CREATING.equals(state) || STATE_RUNNING.equals(state) || STATE_STARTING.equals(state) || STATE_RESTARTING.equals(status))
                return;

            if (STATE_STOPPING.equals(state)) {
                // handle docker restarts
                throw new RetryLater();
            }

            processManager.scheduleProcessInstance(PROCESS_START, instance, noOpProcessData());
        } else if (EVENT_STOP.equals(status) || EVENT_DIE.equals(status)) {
            if (STATE_STOPPED.equals(state) || STATE_STOPPING.equals(state))
                return;

            if (STATE_STARTING.equals(state)) {
                throw new RetryLater();
            }

            processManager.scheduleProcessInstance(PROCESS_STOP, objectManager.reload(instance), noOpProcessData());
        } else if (EVENT_DESTROY.equals(status)) {
            if (REMOVED.equals(state) || REMOVING.equals(state))
                return;

            Map<String, Object> processData = noOpProcessData();
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

    private Long getAccountId(long clusterId, Instance instance) {
        Account owner = objectManager.findAny(Account.class,
                ACCOUNT.CLUSTER_OWNER, true,
                ACCOUNT.CLUSTER_ID, clusterId);
        if (owner == null) {
            log.error("Failed to find account to import container [" + instance.getName() + "][" + instance.getExternalId() + "]");
            return null;
        }

        String namespace = DataAccessor.getLabel(instance, "io.kubernetes.pod.namespace");
        if (StringUtils.isBlank(namespace)) {
            return owner.getId();
        }

        Long accountId = findRancherDefinedNamespace(clusterId, namespace);
        if (accountId != null) {
            return accountId;
        }

        accountId = findBackpopulatedNamespace(clusterId, namespace);
        if (accountId != null) {
            return accountId;
        }

        Account account = clusterDao.createOrGetProjectByName(objectManager.loadResource(io.cattle.platform.core.model.Cluster.class, clusterId),
                namespace, namespace);
        return account.getId();
    }

    private Long findBackpopulatedNamespace(long clusterId, String namespace) {
        Account other = objectManager.findAny(Account.class,
                ACCOUNT.EXTERNAL_ID, namespace,
                ACCOUNT.CLUSTER_ID, clusterId,
                ObjectMetaDataManager.REMOVED_FIELD, null);
        return other == null ? null : other.getId();
    }

    private Long findRancherDefinedNamespace(long clusterId, String namespace) {
        Matcher m = NAMESPACE.matcher(namespace);
        if (m.matches()) {
            Account other = objectManager.findAny(Account.class,
                    ACCOUNT.UUID, Condition.like(m.group(1) + "%"),
                    ACCOUNT.CLUSTER_ID, clusterId,
                    ObjectMetaDataManager.REMOVED_FIELD, null);
            if (other != null) {
                return other.getId();
            }
        }

        return null;
    }

    private void scheduleInstance(ContainerEvent event) {
        Map<String, Object> inspect = event.getDockerInspect();

        Instance instance = objectManager.newRecord(Instance.class);
        instance.setKind(KIND_CONTAINER);
        instance.setExternalId(event.getExternalId());
        instance.setNativeContainer(true);
        instance.setClusterId(event.getClusterId());
        setName(inspect, event, instance);
        setNetwork(inspect, event, instance);
        setImage(event, instance);
        setHost(event, instance);
        setLabels(event, instance);
        setInspect(instance, inspect);
        setHidden(instance);

        Long accountId = getAccountId(event.getClusterId(), instance);
        if (accountId == null) {
            return;
        }

        instance.setAccountId(accountId);
        createAndSchedule(instance, inspect);
    }

    private void setHidden(Instance instance) {
        String podContainerName = DataAccessor.getLabel(instance, SystemLabels.LABEL_K8S_CONTAINER_NAME);
        if (StringUtils.isBlank(podContainerName)) {
            return;
        }

        if (HIDDEN_NAMES.contains(podContainerName)) {
            instance.setHidden(true);
        }
    }

    private void createAndSchedule(Instance instance, Object inspect) {
        String uuid = lookupUuid(instance);
        if (StringUtils.isBlank(uuid)) {
            resourceDao.createAndSchedule(instance, noOpProcessData());
        } else {
            associate(instance, inspect, uuid);
        }
    }

    private void associate(Instance instance, Object inspect, String uuid) {
        Instance existing = objectManager.findAny(Instance.class,
                INSTANCE.REMOVED, null,
                INSTANCE.CLUSTER_ID, instance.getClusterId(),
                INSTANCE.UUID, uuid);
        if (existing == null) {
            sendRemove(instance);
        } else if (isNewer(instance, existing)) {
            instanceLifecycleManager.moveInstance(existing, instance.getExternalId(), instance.getHostId(), inspect);
        } else {
            sendRemove(instance);
        }
    }

    private boolean isNewer(Instance instance, Instance existing) {
        Date created = getCreated(instance);
        if (created == null) {
            return true;
        }

        return created.after(getCreated(existing));
    }

    private Date getCreated(Instance instance) {
        Date created = null;
        Object existingData = CollectionUtils.getNestedValue(instance.getData(), "fields", "dockerInspect", "Created");
        if (existingData != null) {
            try {
                return DateUtils.parse(existingData.toString());
            } catch (Exception ignored) {
            }
        }

        return instance.getCreated();
    }

    private void sendRemove(Instance instance) {
        Long hostId = DataAccessor.fieldLong(instance, FIELD_REQUESTED_HOST_ID);
        instance.setHostId(hostId);

        RemoteAgent agent = agentLocator.lookupAgent(instance);
        DeploymentSyncRequest request = new DeploymentSyncRequest();
        request.setContainers(Collections.singletonList(instance));
        agent.call(EventVO.newEvent("compute.instance.remove")
            .withData(objectSerializer.serialize(request)));
    }

    private String lookupUuid(Instance instance) {
        String uuid = DataAccessor.getLabel(instance, SystemLabels.LABEL_RANCHER_UUID);
        if (StringUtils.isNotBlank(uuid)) {
            return uuid;
        }

        String podContainerName = DataAccessor.getLabel(instance, SystemLabels.LABEL_K8S_CONTAINER_NAME);
        if (StringUtils.isBlank(podContainerName) || podContainerName.length() < 36) {
            return null;
        }

        uuid = podContainerName.substring(podContainerName.length() - 36);
        try {
            UUID.fromString(uuid);
            return uuid;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void setInspect(Instance instance, Object inspect) {
        DataAccessor.setField(instance, InstanceConstants.FIELD_DOCKER_INSPECT, inspect);
    }

    private void setLabels(ContainerEvent event, Instance instance) {
        Map<String, Object> labels = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS);
        if ("true".equals(labels.get(SystemLabels.LABEL_RANCHER_NETWORK))) {
            labels.put(SystemLabels.LABEL_CNI_NETWORK, NetworkConstants.NETWORK_MODE_MANAGED);
        }
        labels.putAll(getLabels(event));
        DataAccessor.setField(instance, InstanceConstants.FIELD_LABELS, labels);
    }

    protected Map<String, Object> noOpProcessData() {
        return CollectionUtils.asMap(PROCESS_DATA_NO_OP, true);
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
        if (parts.length != 2) {
            return null;
        }

        String targetContainer = parts[1];
        Instance netFromInstance = findInstance(instance.getClusterId(), targetContainer);
        if (netFromInstance == null) {
            throw new RetryLater();
        } else {
            DataAccessor.fields(instance).withKey(FIELD_NETWORK_CONTAINER_ID).set(netFromInstance.getId());
            return NetworkConstants.NETWORK_MODE_CONTAINER;
        }
    }

    private String getDockerIp(Map<String, Object> inspect) {
        Object ip = CollectionUtils.getNestedValue(inspect, "NetworkSettings", "IPAddress");
        if (ip != null)
            return ip.toString();

        return null;
    }

    private void setImage(ContainerEvent event, Instance instance) {
        Object imageUuid = CollectionUtils.getNestedValue(event.getDockerInspect(), INSPECT_CONFIG, "Image");
        DataAccessor.fields(instance).withKey(FIELD_IMAGE).set(imageUuid);
    }

    private Map<String, Object> getLabels(ContainerEvent event) {
        return CollectionUtils.toMap(CollectionUtils.getNestedValue(event.getDockerInspect(), INSPECT_CONFIG, INSPECT_LABELS));
    }

    private static class RetryLater extends RuntimeException {
        private static final long serialVersionUID = 7123284316672856140L;
    }
}
