package io.cattle.platform.process.instance;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class InstanceVolumeLookupPreCreate extends AbstractObjectProcessLogic implements ProcessPreListener {

    private static final Logger log = LoggerFactory.getLogger(InstanceVolumeLookupPreCreate.class);

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
     * Looks for shared volumes in the dataVolumes field and converts them to dataVolumeMounts.
     * If volumeDriver is set:
     *  if set to local, do nothing
     *  if set to anything else, restrict volume lookup to a matching storage pool
     *  if blank, do not restrict volume lookup to a particular storage pool.
     * If volume is found, remove from dataVolumes list and put it in dataVolumeMounts. 
     * If volume is not found, just leave it in dataVolumes.
     */
    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance)state.getResource();
        if (!InstanceConstants.KIND_CONTAINER.equals(instance.getKind())) {
            return null;
        }

        String driver = DataAccessor.fieldString(instance, InstanceConstants.FIELD_VOLUME_DRIVER);
        if (VolumeConstants.LOCAL_DRIVER.equals(driver)) {
            return null;
        }

        Map<Object, Object> data = new HashMap<>();
        List<String> dataVolumes = DataAccessor.fieldStringList(instance, InstanceConstants.FIELD_DATA_VOLUMES);
        Map<String, Object> dataVolumeMounts = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_DATA_VOLUME_MOUNTS);
        List<String> newDataVolumes = new ArrayList<String>();
        data.put(InstanceConstants.FIELD_DATA_VOLUMES, newDataVolumes);
        data.put(InstanceConstants.FIELD_DATA_VOLUME_MOUNTS, dataVolumeMounts);
        for (String v : dataVolumes) {
            if (!v.startsWith("/")) {
                String[] parts = v.split(":", 2);
                if (parts.length > 1) {
                    try {
                        Volume vol = volumeDao.findSharedVolume(instance.getAccountId(), driver, parts[0]);
                        if (vol != null) {
                            dataVolumeMounts.put(parts[1], vol.getId());
                            continue;
                        }
                    } catch (IllegalStateException e) {
                        log.error(e.getMessage());
                        objectProcessManager.scheduleProcessInstance(InstanceConstants.PROCESS_REMOVE, instance, null);
                        ExecutionException ex = new ExecutionException(String.format("Could not process named volume %s. "
                                + "More than one volume with that name exists.", parts[0]));
                        ex.setResources(state.getResource());
                        throw ex;
                    }
                }
            }
            newDataVolumes.add(v);
        }
        return new HandlerResult(data);
    }
}
