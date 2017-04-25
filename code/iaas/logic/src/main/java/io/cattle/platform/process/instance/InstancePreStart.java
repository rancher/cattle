package io.cattle.platform.process.instance;

import static io.cattle.platform.core.model.tables.VolumeTable.*;

import io.cattle.platform.allocator.constraint.HostAffinityConstraint;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.StoragePoolDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.util.InstanceHelpers;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.process.lock.InstanceVolumeAccessModeLock;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

@Named
public class InstancePreStart extends AbstractObjectProcessLogic implements ProcessPreListener {

    @Inject
    LockManager lockManager;

    @Inject
    StoragePoolDao storagePoolDao;


    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        final Instance instance = (Instance)state.getResource();
        Map<String, Object> labels = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS);

        List<Volume> volumes = InstanceHelpers.extractVolumesFromMounts(instance, objectManager);
        for (final Volume v : volumes) {
            String driver = DataAccessor.fieldString(v, VolumeConstants.FIELD_VOLUME_DRIVER);
            if (StringUtils.isNotEmpty(driver) && !VolumeConstants.LOCAL_DRIVER.equals(driver)) {
                List<? extends StoragePool> pools = storagePoolDao.findStoragePoolByDriverName(v.getAccountId(), driver);
                if (pools.size() == 0) {
                    continue;
                }
                StoragePool sp = pools.get(0);

                final String accessMode = sp.getVolumeAccessMode();
                if (StringUtils.isNotEmpty(accessMode) && StringUtils.isEmpty(v.getAccessMode()) && !accessMode.equals(v.getAccessMode())) {
                    lockManager.lock(new InstanceVolumeAccessModeLock(v.getId()), new LockCallbackNoReturn() {
                        @Override
                        public void doWithLockNoResult() {
                            objectManager.setFields(v, VOLUME.ACCESS_MODE, accessMode);
                        }
                    });
                }
            }
            Map<String, Object> driver_opts = DataAccessor.fieldMap(v, VolumeConstants.FIELD_VOLUME_DRIVER_OPTS);
            if (driver_opts.containsKey(VolumeConstants.EC2_AZ)) {
                String zone = driver_opts.get(VolumeConstants.EC2_AZ).toString();
                labels.put(HostAffinityConstraint.LABEL_HEADER_AFFINITY_HOST_LABEL, String.format("%s=%s", VolumeConstants.EC2_AZ_HOST_LABEL_KEY, zone));
                objectManager.setFields(instance, InstanceConstants.FIELD_LABELS, labels);
            }
        }

        return null;
    }

    @Override
    public String[] getProcessNames() {
        return new String[] { "instance.start" };
    }
}
