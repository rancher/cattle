package io.cattle.platform.process.containerevent;

import static io.cattle.platform.core.model.tables.InstanceTable.*;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.ContainerEvent;
import io.cattle.platform.core.model.Image;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.storage.service.StorageService;
import io.cattle.platform.util.exception.ExecutionException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.BooleanUtils;

import com.netflix.config.DynamicBooleanProperty;

@Named
public class ContainerEventCreate extends AbstractDefaultProcessHandler {

    private static final DynamicBooleanProperty MANAGE_NONRANCHER_CONTAIERS = ArchaiusUtil
            .getBoolean("manage.nonrancher.containers");
    private static final String INSPECT_ENV = "Env";
    private static final String INSPECT_NAME = "Name";
    private static final String FIELD_DOCKER_INSPECT = "dockerInspect";
    private static final String INSPECT_CONFIG = "Config";
    private static final String INSPECT_IMAGE = "Image";
    private static final String IMAGE_PREFIX = "docker:";
    private static final String IMAGE_KIND_PATTERN = "^(sim|docker):.*";
    private static final String RANCHER_UUID = "RANCHER_UUID=";
    private static final String RANCHER_NETWORK = "RANCHER_NETWORK=";

    @Inject
    StorageService storageService;

    @Inject
    NetworkDao networkDao;

    @Inject
    AccountDao accountDao;

    @Inject
    LockManager lockManager;

    @SuppressWarnings("unchecked")
    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        if ( !MANAGE_NONRANCHER_CONTAIERS.get() ) {
            return null;
        }

        ContainerEvent event = (ContainerEvent)state.getResource();
        Map<String, Object> inspect = (Map<String, Object>)DataUtils.getFields(event).get(FIELD_DOCKER_INSPECT);

        String rancherUuid = getRancherUuidLabel(inspect);
        if ( rancherUuid != null ) {
            return null;
        }

        scheduleInstance(event, inspect);

        return null;
    }

    void scheduleInstance(final ContainerEvent event, final Map<String, Object> inspect) {
        final Long accountId = event.getAccountId();
        final String externalId = event.getExternalId();

        lockManager.lock(new ContainerEventInstanceLock(accountId, externalId), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                // TODO INDEX ON EXTERNAL ID
                Instance instance = objectManager.findAny(Instance.class, INSTANCE.ACCOUNT_ID, accountId,
                        INSTANCE.EXTERNAL_ID, externalId);
                if ( instance == null ) {
                    instance = objectManager.newRecord(Instance.class);
                    instance.setKind(InstanceConstants.KIND_CONTAINER);
                    instance.setAccountId(accountId);
                    instance.setExternalId(externalId);
                    setName(inspect, instance);
                    setNetwork(accountId, inspect, instance);
                    setImage(inspect, instance);
                    setHost(event, instance);
                    instance = objectManager.create(instance);
                }

                if ( !InstanceConstants.STATE_RUNNING.equals(ObjectUtils.getState(instance)) ) {
                    createIgnoreCancel(instance, null);
                    getObjectProcessManager().scheduleProcessInstance(InstanceConstants.PROCESS_START, instance, null);
                }
            }
        });
    }

    void setHost(ContainerEvent event, Instance instance) {
        DataAccessor.fromDataFieldOf(instance).withKey(InstanceConstants.FIELD_REQUESTED_HOST_ID)
                .set(event.getHostId());
    }

    void setName(Map<String, Object> inspect, Instance instance) {
        String name = (String)inspect.get(INSPECT_NAME);
        name = name.replaceFirst("/", "");
        instance.setName(name);
    }

    void setNetwork(Long accountId, Map<String, Object> inspect, Instance instance) {
        String networkKind = NetworkConstants.KIND_NETWORK;
        if ( BooleanUtils.toBoolean(getRancherNetworkLabel(inspect)) ) {
            networkKind = NetworkConstants.KIND_HOSTONLY;
        }

        List<? extends Network> networks = networkDao.getNetworksForAccount(accountId, networkKind);
        if ( networks.isEmpty() ) {
            networks = networkDao.getNetworksForAccount(accountDao.getSystemAccount().getId(), networkKind);
        }

        Long networkId = !networks.isEmpty() ? networks.get(0).getId() : null;

        if ( networkId != null ) {
            List<Long> netIds = new ArrayList<Long>();
            netIds.add(networkId);
            DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_NETWORK_IDS).set(netIds);
        }
    }

    @SuppressWarnings("unchecked")
    void setImage(Map<String, Object> inspect, Instance instance) {
        Map<String, Object> config = (Map<String, Object>)inspect.get(INSPECT_CONFIG);
        String imageUuid = (String)config.get(INSPECT_IMAGE);

        // Somewhat of a hack, but needed for testing against sim contexts
        if ( !imageUuid.matches(IMAGE_KIND_PATTERN) ) {
            imageUuid = IMAGE_PREFIX + imageUuid;
        }
        DataAccessor.fromDataFieldOf(instance).withKey(InstanceConstants.FIELD_IMAGE_UUID).set(imageUuid);
        Image image = null;
        try {
            image = storageService.registerRemoteImage(imageUuid);
        } catch (IOException e) {
            throw new ExecutionException("Unable to create image.", e);
        }
        instance.setImageId(image.getId());
    }

    String getRancherUuidLabel(Map<String, Object> inspect) {
        return getLabel(RANCHER_UUID, inspect);
    }

    String getRancherNetworkLabel(Map<String, Object> inspect) {
        return getLabel(RANCHER_NETWORK, inspect);
    }

    @SuppressWarnings("unchecked")
    String getLabel(String key, Map<String, Object> inspect) {
        try {
            Map<String, Object> config = (Map<String, Object>)inspect.get(INSPECT_CONFIG);
            List<String> envVars = (List<String>)config.get(INSPECT_ENV);
            for ( String envVar : envVars ) {
                if ( envVar.startsWith(key) ) {
                    return envVar.substring(key.length());
                }
            }
        } catch (NullPointerException e) {

        }
        return null;
    }
}
