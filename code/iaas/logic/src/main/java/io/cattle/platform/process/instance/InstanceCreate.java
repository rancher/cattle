package io.cattle.platform.process.instance;

import static io.cattle.platform.core.model.tables.CredentialInstanceMapTable.*;
import static io.cattle.platform.core.model.tables.NicTable.*;
import static io.cattle.platform.core.model.tables.VolumeTable.*;

import io.cattle.iaas.labels.service.LabelsService;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.LabelsDao;
import io.cattle.platform.core.model.CredentialInstanceMap;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class InstanceCreate extends AbstractDefaultProcessHandler {

    @Inject
    LabelsService labelsService;

    @Inject
    GenericResourceDao resourceDao;
    @Inject
    JsonMapper jsonMapper;
    @Inject
    ObjectProcessManager processManager;

    @Inject
    LabelsDao labelsDao;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        setCreateStart(state);

        Instance instance = (Instance) state.getResource();

        List<Volume> volumes = objectManager.children(instance, Volume.class);
        List<String> dataVolumes = processManagedVolumes(instance);
        List<Nic> nics = objectManager.children(instance, Nic.class);

        Set<Long> creds = createCreds(instance, state.getData());
        Set<Long> volumesIds = createVolumes(instance, volumes, state.getData());
        Set<Long> nicIds = createNics(instance, nics, state.getData());

        HandlerResult result = new HandlerResult("_volumeIds", volumesIds, "_nicIds", nicIds, "_creds", creds, InstanceConstants.FIELD_DATA_VOLUMES,
                dataVolumes);
        result.shouldDelegate(shouldStart(instance));

        createLabels(instance);

        return result;
    }

    private List<String> processManagedVolumes(Instance instance) {
        List<String> dataVolumes = DataAccessor.fieldStringList(instance, InstanceConstants.FIELD_DATA_VOLUMES);
        if (dataVolumes == null) {
            dataVolumes = new ArrayList<String>();
        }
        Map<String, Object> dataVolumeMounts = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_DATA_VOLUME_MOUNTS);
        if (dataVolumeMounts == null) {
            return dataVolumes;
        }
        for (Map.Entry<String, Object> mountedVols : dataVolumeMounts.entrySet()) {
            Long volId = ((Number) mountedVols.getValue()).longValue();
            Volume vol = objectManager.loadResource(Volume.class, volId);
            if (vol != null) {
                String volDescriptor = vol.getName() + ":" + mountedVols.getKey().trim();
                if (!dataVolumes.contains(volDescriptor)) {
                    dataVolumes.add(volDescriptor);
                }
            }
        }
        return dataVolumes;
    }

    protected boolean shouldStart(Instance instance) {
        return DataAccessor.fields(instance)
                .withKey(InstanceConstants.FIELD_START_ON_CREATE)
                .withDefault(true)
                .as(Boolean.class);
    }

    private void createLabels(Instance instance) {
        @SuppressWarnings("unchecked")
        Map<String, String> labels = DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_LABELS).as(Map.class);
        if (labels == null) {
            return;
        }

        for (Map.Entry<String, String> labelEntry : labels.entrySet()) {
            String labelKey = labelEntry.getKey();
            String labelValue = labelEntry.getValue();

            labelsService.createContainerLabel(instance.getAccountId(), instance.getId(), labelKey, labelValue);
        }
    }

    protected Set<Long> createVolumes(Instance instance, List<Volume> volumes, Map<String, Object> data) {
        Set<Long> volumeIds = new TreeSet<Long>();
        Volume root = createRoot(instance, volumes, data);
        if (root != null) {
            volumeIds.add(root.getId());
        }
        return volumeIds;
    }

    protected Volume createRoot(Instance instance, List<Volume> volumes, Map<String, Object> data) {
        Volume root = getRoot(instance, volumes);
        if (root == null) {
            return null;
        }
        processManager.executeStandardProcess(StandardProcess.CREATE, root, data);
        return root;
    }

    protected Volume getRoot(Instance instance, List<Volume> volumes) {
        if (instance.getImageId() == null) {
            return null;
        }

        for (Volume volume : volumes) {
            if (volume.getDeviceNumber() != null && volume.getDeviceNumber().intValue() == 0) {
                return volume;
            }
        }

        return objectManager.create(Volume.class, VOLUME.ACCOUNT_ID, instance.getAccountId(), VOLUME.INSTANCE_ID, instance.getId(), VOLUME.IMAGE_ID, instance
                .getImageId(), VOLUME.DEVICE_NUMBER, 0, VOLUME.ATTACHED_STATE, CommonStatesConstants.ACTIVE);
    }

    protected Set<Long> createNics(Instance instance, List<Nic> nics, Map<String, Object> data) {
        List<Long> networkIds = populateNetworks(instance);
        return createNicsFromIds(instance, nics, data, networkIds);
    }

    protected List<Long> populateNetworks(Instance instance) {
        return DataUtils.getFieldList(instance.getData(), InstanceConstants.FIELD_NETWORK_IDS, Long.class);
    }

    protected Set<Long> createNicsFromIds(Instance instance, List<Nic> nics, Map<String, Object> data, List<Long> networkIds) {
        Set<Long> nicIds = new TreeSet<Long>();

        int deviceId = 0;

        if (networkIds != null) {
            for (int i = 0; i < networkIds.size(); i++) {
                Number createId = networkIds.get(i);
                if (createId == null) {
                    deviceId++;
                    continue;
                }

                Nic newNic = null;
                for (Nic nic : nics) {
                    if (nic.getNetworkId() == createId.longValue()) {
                        newNic = nic;
                        break;
                    }
                }

                if (newNic == null) {
                    newNic = objectManager.create(Nic.class, NIC.ACCOUNT_ID, instance.getAccountId(), NIC.NETWORK_ID, createId, NIC.INSTANCE_ID, instance
                            .getId(), NIC.DEVICE_NUMBER, deviceId);
                }

                deviceId++;

                processManager.executeStandardProcess(StandardProcess.CREATE, newNic, data);
                nicIds.add(newNic.getId());
            }
        }

        return nicIds;
    }

    protected Set<Long> createCreds(Instance instance, Map<String, Object> data) {
        List<Long> credIds = DataUtils.getFieldList(instance.getData(), InstanceConstants.FIELD_CREDENTIAL_IDS, Long.class);

        return createCredsFromIds(credIds, instance, data);
    }

    protected Set<Long> createCredsFromIds(List<Long> credIds, Instance instance, Map<String, Object> data) {
        if (credIds == null) {
            return Collections.emptySet();
        }

        Set<Long> created = new HashSet<Long>();
        List<CredentialInstanceMap> maps = new ArrayList<CredentialInstanceMap>();

        for (CredentialInstanceMap map : children(instance, CredentialInstanceMap.class)) {
            maps.add(map);
            created.add(map.getCredentialId());
        }

        for (Long credId : credIds) {
            if (!created.contains(credId)) {
                maps.add(objectManager.create(CredentialInstanceMap.class, CREDENTIAL_INSTANCE_MAP.INSTANCE_ID, instance.getId(),
                        CREDENTIAL_INSTANCE_MAP.CREDENTIAL_ID, credId));
            }
        }

        for (CredentialInstanceMap map : maps) {
            createThenActivate(map, data);
        }

        return new TreeSet<Long>(credIds);
    }

    public static boolean isCreateStart(ProcessState state) {
        Boolean startOnCreate = DataAccessor.fromMap(state.getData()).withScope(InstanceCreate.class).withKey(InstanceConstants.FIELD_START_ON_CREATE).as(
                Boolean.class);

        return startOnCreate == null ? false : startOnCreate;
    }

    protected void setCreateStart(ProcessState state) {
        DataAccessor.fromMap(state.getData()).withScope(InstanceCreate.class).withKey(InstanceConstants.FIELD_START_ON_CREATE).set(true);
    }

}
