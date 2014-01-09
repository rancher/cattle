package io.github.ibuildthecloud.dstack.process.instance;

import static io.github.ibuildthecloud.dstack.core.model.tables.NicTable.*;
import static io.github.ibuildthecloud.dstack.core.model.tables.VolumeTable.*;
import io.github.ibuildthecloud.dstack.core.constants.CommonStatesConstants;
import io.github.ibuildthecloud.dstack.core.constants.InstanceConstants;
import io.github.ibuildthecloud.dstack.core.model.Instance;
import io.github.ibuildthecloud.dstack.core.model.Nic;
import io.github.ibuildthecloud.dstack.core.model.Volume;
import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.json.JsonMapper;
import io.github.ibuildthecloud.dstack.object.ObjectManager;
import io.github.ibuildthecloud.dstack.object.process.ObjectProcessManager;
import io.github.ibuildthecloud.dstack.object.process.StandardProcess;
import io.github.ibuildthecloud.dstack.object.util.DataUtils;
import io.github.ibuildthecloud.dstack.process.base.AbstractDefaultProcessHandler;
import io.github.ibuildthecloud.dstack.transport.TransportFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class InstanceCreate extends AbstractDefaultProcessHandler {

    JsonMapper jsonMapper;
    TransportFactory transportFactory;
    ObjectProcessManager processManager;
    ObjectManager objectManager;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance)state.getResource();
        List<Volume> volumes = objectManager.children(instance, Volume.class);
        List<Nic> nics = objectManager.children(instance, Nic.class);

        Set<Long> volumesIds = createVolumes(instance, volumes, state.getData());
        Set<Long> nicIds = createNics(instance, nics, state.getData());

        start(instance);

        return new HandlerResult("_volumeIds", volumesIds, "_nicIds", nicIds);
    }

    protected void start(Instance instance) {
        Boolean doneStart = DataUtils.getField(instance.getData(), InstanceConstants.FIELD_START_ON_CREATE, Boolean.class);

        if ( doneStart != null && ! doneStart.booleanValue() ) {
            return;
        }

        ProcessInstance process = processManager.createProcessInstance("instance.start", instance, null);
        process.execute();
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
        if ( networkIds == null )
            return Collections.emptySet();

        Set<Long> nicIds = new TreeSet<Long>();

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
                        NIC.DEVICE_NUMBER, i);

            }

            processManager.executeStandardProcess(StandardProcess.CREATE, newNic, data);
            nicIds.add(newNic.getId());
        }

        return nicIds;
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public TransportFactory getTransportFactory() {
        return transportFactory;
    }

    @Inject
    public void setTransportFactory(TransportFactory transportFactory) {
        this.transportFactory = transportFactory;
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
