package io.cattle.platform.docker.process.containerevent;

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
import io.cattle.platform.docker.transform.DockerTransformer;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.BooleanUtils;

import com.netflix.config.DynamicBooleanProperty;

public class ContainerEventCreate extends AbstractDefaultProcessHandler {

    private static final DynamicBooleanProperty MANAGE_NONRANCHER_CONTAINERS = ArchaiusUtil
            .getBoolean("manage.nonrancher.containers");
    private static final String INSPECT_ENV = "Env";
    private static final String FIELD_DOCKER_INSPECT = "dockerInspect";
    private static final String INSPECT_CONFIG = "Config";
    
    private static final String RANCHER_UUID = "RANCHER_UUID=";
    private static final String RANCHER_NETWORK = "RANCHER_NETWORK=";

    @Inject
    StorageService storageService;

    @Inject
    NetworkDao networkDao;

    @Inject
    AccountDao accountDao;
    
    @Inject
    DockerTransformer dockerTransformer;

    @Inject
    LockManager lockManager;

    @SuppressWarnings("unchecked")
    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        if ( !MANAGE_NONRANCHER_CONTAINERS.get() ) {
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
                Instance instance = objectManager.findAny(Instance.class, INSTANCE.ACCOUNT_ID, accountId,
                        INSTANCE.EXTERNAL_ID, externalId);
                if ( instance == null ) {
                    instance = objectManager.newRecord(Instance.class);
                    instance.setKind(InstanceConstants.KIND_CONTAINER);
                    dockerTransformer.transform(inspect, instance);
                    instance.setAccountId(accountId);
                    instance.setNativeContainer(true);
                    setNetwork(inspect, instance);
                    setImage(instance);
                    setHost(event, instance);
                    instance = objectManager.create(instance);
                }

                if ( !InstanceConstants.STATE_RUNNING.equals(ObjectUtils.getState(instance)) &&
                        !InstanceConstants.STATE_STARTING.equals(ObjectUtils.getState(instance))) {
                    try {
                        getObjectProcessManager().scheduleProcessInstance(InstanceConstants.PROCESS_CREATE, instance, makeData());
                    } catch (ProcessCancelException e) {
                        // ignore
                    }
                }
            }
        });
    }

    protected Map<String, Object> makeData() {
        Map<String, Object> data = new HashMap<String, Object>();
        DataAccessor.fromMap(data).withKey(InstanceConstants.PROCESS_DATA_NO_OP).set(true);
        return data;
    }

    void setHost(ContainerEvent event, Instance instance) {
        DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_REQUESTED_HOST_ID)
                .set(event.getHostId());
    }

    void setNetwork(Map<String, Object> inspect, Instance instance) {
        String networkKind = NetworkConstants.KIND_NETWORK;
        if ( BooleanUtils.toBoolean(getRancherNetworkLabel(inspect)) ) {
            networkKind = NetworkConstants.KIND_HOSTONLY;
        }
        Network network = networkDao.getNetworkForObject(instance, networkKind);

        if ( network != null ) {
            List<Long> netIds = new ArrayList<Long>();
            netIds.add(network.getId());
            DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_NETWORK_IDS).set(netIds);
        }
    }

    @SuppressWarnings("unchecked")
    void setImage(Instance instance) {
        String imageUuid = (String)DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_IMAGE_UUID).get();
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
        Map<String, Object> config = (Map<String, Object>)inspect.get(INSPECT_CONFIG);
        List<String> envVars = (List<String>)config.get(INSPECT_ENV);
        if ( envVars == null ) {
            return null;
        }
        for ( String envVar : envVars ) {
            if ( envVar.startsWith(key) ) {
                return envVar.substring(key.length());
            }
        }
        return null;
    }
}
