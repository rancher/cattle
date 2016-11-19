package io.cattle.platform.process.instance;

import static io.cattle.platform.core.model.tables.VolumeTable.*;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.StoragePoolDao;
import io.cattle.platform.core.dao.VolumeDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.exception.ExecutionException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

@Named
public class InstanceVolumeLookupPreCreate extends AbstractObjectProcessLogic implements ProcessPreListener {

    private static final String LABEL_VOLUME_AFFINITY = "io.rancher.scheduler.affinity:volumes";

    @Inject
    ObjectManager objectManager;
    @Inject
    ObjectProcessManager processManager;
    @Inject
    StoragePoolDao storagePoolDao;
    @Inject
    VolumeDao volumeDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { "instance.create" };
    }

    /**
     * Looks for volumes in the dataVolumes field and converts them to dataVolumeMounts.
     * Will convert if the volume name matches a shared volume or an unmapped volume (one with no VolumeStoragePoolMap entry).
     * Additionally, will map volumes found in "local" pools if the name appears in the io.rancher.scheduler.affinity:volumes label.
     */
    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance)state.getResource();
        if (!InstanceConstants.CONTAINER_LIKE.contains(instance.getKind())) {
            return null;
        }

        Object va = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS).get(LABEL_VOLUME_AFFINITY);
        Set<String> affinities = new HashSet<String>();
        if (va != null) {
            affinities.addAll(Arrays.asList(va.toString().split(",")));
        }

        String volumeDriver = DataAccessor.fieldString(instance, InstanceConstants.FIELD_VOLUME_DRIVER);

        Map<Object, Object> data = new HashMap<>();
        List<String> dataVolumes = DataAccessor.fieldStringList(instance, InstanceConstants.FIELD_DATA_VOLUMES);
        Map<String, Object> dataVolumeMounts = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_DATA_VOLUME_MOUNTS);
        List<String> newDataVolumes = new ArrayList<String>();
        data.put(InstanceConstants.FIELD_DATA_VOLUMES, newDataVolumes);
        data.put(InstanceConstants.FIELD_DATA_VOLUME_MOUNTS, dataVolumeMounts);
        for (String v : dataVolumes) {
            String volName = null;
            String volPath = null;
            String[] parts = v.split(":", 2);
             if (parts.length == 2 && !parts[0].startsWith("/") && parts[1].startsWith("/")) {
                // named volume
                 volName = parts[0];
                 volPath = parts[1];
            } else if (isNonlocalDriver(volumeDriver) && parts.length == 2 && parts[0].startsWith("/") && !parts[1].startsWith("/")) {
                // anonymous volume with mount permissions
                volName = UUID.randomUUID().toString();
                volPath = parts[0] + ":" + parts[1];
                v = volName + ":" + volPath;
            } else if (isNonlocalDriver(volumeDriver) && parts.length == 1 && parts[0].startsWith("/")) {
                // anonymous volume
                volName = UUID.randomUUID().toString();
                volPath = parts[0];
                v = volName + ":" + volPath;
            } else {
                newDataVolumes.add(v);
                continue;
            }

            boolean createVol = true;
            if (volName != null) {
                List<? extends Volume> volumes = volumeDao.findSharedOrUnmappedVolumes(instance.getAccountId(), volName);
                if (volumes.isEmpty() && affinities.contains(volName)) {
                    // Any volumes found here will be mapped to local (docker, sim) pools
                    volumes = objectManager.find(Volume.class, VOLUME.ACCOUNT_ID, instance.getAccountId(), VOLUME.NAME, volName, VOLUME.REMOVED, null);
                }

                if (volumes.size() == 1) {
                    dataVolumeMounts.put(volPath, volumes.get(0).getId());
                    createVol = false;
                } else if (volumes.size() > 1) {
                    objectProcessManager.scheduleProcessInstance(InstanceConstants.PROCESS_REMOVE, instance, null);
                    ExecutionException ex =
                            new ExecutionException(
                                    String.format("Could not process named volume %s. More than one volume " + "with that name exists.", volName));
                    ex.setResources(state.getResource());
                    throw ex;
                }
            }

            if (createVol && isNonlocalDriver(volumeDriver)) {
                Volume newVol = volumeDao.createVolumeForDriver(instance.getAccountId(), volName, volumeDriver);
                dataVolumeMounts.put(volPath, newVol.getId());
            }
            newDataVolumes.add(v);
        }
        return new HandlerResult(data);
    }

    boolean isNonlocalDriver(String volumeDriver) {
        return StringUtils.isNotEmpty(volumeDriver) && !VolumeConstants.LOCAL_DRIVER.equals(volumeDriver);
    }
}
