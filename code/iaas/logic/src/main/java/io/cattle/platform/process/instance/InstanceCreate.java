package io.cattle.platform.process.instance;

import static io.cattle.platform.core.model.tables.NicTable.*;
import static io.cattle.platform.core.model.tables.VolumeTable.*;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.core.model.Vnet;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class InstanceCreate extends AbstractDefaultProcessHandler {

    JsonMapper jsonMapper;
    ObjectProcessManager processManager;
    ObjectManager objectManager;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        setCreateStart(state);

        Instance instance = (Instance)state.getResource();
        List<Volume> volumes = objectManager.children(instance, Volume.class);
        List<Nic> nics = objectManager.children(instance, Nic.class);

        Set<Long> volumesIds = createVolumes(instance, volumes, state.getData());
        Set<Long> nicIds = createNics(instance, nics, state.getData());

        HandlerResult result = new HandlerResult("_volumeIds", volumesIds, "_nicIds", nicIds);
        result.shouldDelegate(shouldStart(instance));

        return result;
    }

    protected boolean shouldStart(Instance instance) {
        Boolean doneStart = DataAccessor.fields(instance)
                                .withKey(InstanceConstants.FIELD_START_ON_CREATE)
                                .withDefault(true)
                                .as(Boolean.class);

        if ( doneStart != null && ! doneStart.booleanValue() ) {
            return false;
        } else {
            return true;
        }
    }

    protected Set<Long> createVolumes(Instance instance, List<Volume> volumes, Map<String,Object> data) {
        Set<Long> volumeIds = new TreeSet<Long>();

        long deviceId = 0;
        Volume root = createRoot(instance, volumes, data);
        if ( root != null ) {
            volumeIds.add(root.getId());
            deviceId++;
        }

        List<Long> volumeOfferingIds = DataUtils.getFieldList(instance.getData(), InstanceConstants.FIELD_VOLUME_OFFERING_IDS, Long.class);
        if ( volumeOfferingIds == null ) {
            volumeOfferingIds = new ArrayList<Long>();
        }

        for ( int i = 0 ; i < volumeOfferingIds.size() ; i++ ) {
            long deviceNumber = deviceId + i;
            Volume newVolume = null;
            for ( Volume volume : volumes ) {
                if ( volume.getDeviceNumber().intValue() == deviceNumber ) {
                    newVolume = volume;
                    break;
                }
            }

            if ( newVolume == null ) {
                newVolume = objectManager.create(Volume.class,
                        VOLUME.ACCOUNT_ID, instance.getAccountId(),
                        VOLUME.INSTANCE_ID, instance.getId(),
                        VOLUME.OFFERING_ID, volumeOfferingIds.get(i),
                        VOLUME.DEVICE_NUMBER, deviceNumber,
                        VOLUME.ATTACHED_STATE, CommonStatesConstants.ACTIVE
                        );
            }

            volumeIds.add(newVolume.getId());
        }

        return volumeIds;
    }

    protected Volume createRoot(Instance instance, List<Volume> volumes, Map<String,Object> data) {
        Volume root = getRoot(instance, volumes);
        if ( root == null ) {
            return null;
        }
        processManager.executeStandardProcess(StandardProcess.CREATE, root, data);
        return root;
    }

    protected Volume getRoot(Instance instance, List<Volume> volumes) {
        if ( instance.getImageId() == null ) {
            return null;
        }

        for ( Volume volume : volumes ) {
            if ( volume.getDeviceNumber().intValue() == 0 ) {
                return volume;
            }
        }

        return objectManager.create(Volume.class,
                VOLUME.ACCOUNT_ID, instance.getAccountId(),
                VOLUME.INSTANCE_ID, instance.getId(),
                VOLUME.IMAGE_ID, instance.getImageId(),
                VOLUME.DEVICE_NUMBER, 0,
                VOLUME.ATTACHED_STATE, CommonStatesConstants.ACTIVE
                );
    }

    protected Set<Long> createNics(Instance instance, List<Nic> nics, Map<String,Object> data) {
        List<Long> networkIds = DataUtils.getFieldList(instance.getData(), InstanceConstants.FIELD_NETWORK_IDS, Long.class);
        List<Long> subnetIds = DataUtils.getFieldList(instance.getData(), InstanceConstants.FIELD_SUBNET_IDS, Long.class);
        List<Long> vnetIds = DataUtils.getFieldList(instance.getData(), InstanceConstants.FIELD_VNET_IDS, Long.class);

        Set<Long> nicIds = new TreeSet<Long>();

        int deviceId = 0;

        if ( networkIds != null ) {
            for ( int i = 0 ; i < networkIds.size() ; i++ ) {
                Number createId = networkIds.get(i);
                Nic newNic = null;
                for ( Nic nic : nics ) {
                    if ( nic.getNetworkId() == createId.longValue() ) {
                        newNic = nic;
                        break;
                    }
                }

                if ( newNic == null ) {
                    newNic = objectManager.create(Nic.class,
                            NIC.ACCOUNT_ID, instance.getAccountId(),
                            NIC.NETWORK_ID, createId,
                            NIC.INSTANCE_ID, instance.getId(),
                            NIC.DEVICE_NUMBER, deviceId);
                }

                deviceId++;

                processManager.executeStandardProcess(StandardProcess.CREATE, newNic, data);
                nicIds.add(newNic.getId());
            }
        }

        if ( subnetIds != null ) {
            for ( int i = 0 ; i < subnetIds.size() ; i++ ) {
                Number createId = subnetIds.get(i);
                Subnet subnet = objectManager.loadResource(Subnet.class, createId.toString());

                if ( subnet == null ) {
                    return null;
                }

                Nic newNic = null;
                for ( Nic nic : nics ) {
                    if ( nic.getSubnetId() == createId.longValue() ) {
                        newNic = nic;
                        break;
                    }
                }

                if ( newNic == null ) {
                    newNic = objectManager.create(Nic.class,
                            NIC.ACCOUNT_ID, instance.getAccountId(),
                            NIC.NETWORK_ID, subnet.getNetworkId(),
                            NIC.SUBNET_ID, createId,
                            NIC.INSTANCE_ID, instance.getId(),
                            NIC.DEVICE_NUMBER, deviceId);

                }

                deviceId++;

                processManager.executeStandardProcess(StandardProcess.CREATE, newNic, data);
                nicIds.add(newNic.getId());
            }
        }

        if ( vnetIds != null ) {
            for ( int i = 0 ; i < vnetIds.size() ; i++ ) {
                Number createId = vnetIds.get(i);
                Vnet vnet = objectManager.loadResource(Vnet.class, createId.toString());

                if ( vnet == null ) {
                    return null;
                }

                Nic newNic = null;
                for ( Nic nic : nics ) {
                    if ( nic.getVnetId() == createId.longValue() ) {
                        newNic = nic;
                        break;
                    }
                }

                if ( newNic == null ) {
                    newNic = objectManager.create(Nic.class,
                            NIC.ACCOUNT_ID, instance.getAccountId(),
                            NIC.NETWORK_ID, vnet.getNetworkId(),
                            NIC.VNET_ID, createId,
                            NIC.INSTANCE_ID, instance.getId(),
                            NIC.DEVICE_NUMBER, deviceId);

                }

                deviceId++;

                processManager.executeStandardProcess(StandardProcess.CREATE, newNic, data);
                nicIds.add(newNic.getId());
            }
        }

        return nicIds;
    }

    public static boolean isCreateStart(ProcessState state) {
        Boolean startOnCreate = DataAccessor.fromMap(state.getData())
                .withScope(InstanceCreate.class)
                .withKey(InstanceConstants.FIELD_START_ON_CREATE)
                .as(Boolean.class);

        return startOnCreate == null ? false : startOnCreate;
    }

    protected void setCreateStart(ProcessState state) {
        DataAccessor.fromMap(state.getData())
            .withScope(InstanceCreate.class)
            .withKey(InstanceConstants.FIELD_START_ON_CREATE)
            .set(true);
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Override
    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    public ObjectProcessManager getProcessManager() {
        return processManager;
    }

    @Inject
    public void setProcessManager(ObjectProcessManager processManager) {
        this.processManager = processManager;
    }

}
