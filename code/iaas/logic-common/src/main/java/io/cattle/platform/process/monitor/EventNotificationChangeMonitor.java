package io.cattle.platform.process.monitor;

import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.Mount;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.StoragePoolHostMap;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeStoragePoolMap;
import io.cattle.platform.engine.manager.impl.ProcessRecord;
import io.cattle.platform.engine.process.ProcessServiceContext;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.ObjectUtils;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class EventNotificationChangeMonitor extends io.cattle.platform.engine.process.impl.EventNotificationChangeMonitor {

    @Inject
    ObjectManager objectManager;

    @Override
    public void addData(Map<String, Object> data, boolean schedule, String previousState, String newState, ProcessRecord record, ProcessState state,
            ProcessServiceContext context) {
        Object resource = state.getResource();
        Object accountId = ObjectUtils.getAccountId(resource);

        if (accountId != null) {
            data.put(ObjectMetaDataManager.ACCOUNT_FIELD, accountId);
        }

        super.addData(data, schedule, previousState, newState, record, state, context);

        Object obj = state.getResource();
        if (obj instanceof Service) {
            sendChange(Stack.class, accountId, ((Service) obj).getStackId(), schedule, context);
        } else if (obj instanceof ServiceConsumeMap) {
            sendChange(Service.class, accountId, ((ServiceConsumeMap) obj).getServiceId(), schedule, context);
            // accountId is null to handle cross environment service links case
            sendChange(Service.class, null, ((ServiceConsumeMap) obj).getConsumedServiceId(), schedule, context);
        } else if (obj instanceof ServiceExposeMap) {
            sendChange(Service.class, accountId, ((ServiceExposeMap) obj).getServiceId(), schedule, context);
            sendChange(Instance.class, accountId, ((ServiceExposeMap) obj).getInstanceId(), schedule, context);
        } else if (obj instanceof InstanceHostMap) {
            sendChange(Host.class, accountId, ((InstanceHostMap) obj).getHostId(), schedule, context);
            sendChange(Instance.class, accountId, ((InstanceHostMap) obj).getInstanceId(), schedule, context);
        } else if (obj instanceof StoragePoolHostMap) {
            sendChange(StoragePool.class, accountId, ((StoragePoolHostMap) obj).getStoragePoolId(), schedule, context);
            sendChange(Host.class, accountId, ((StoragePoolHostMap) obj).getHostId(), schedule, context);
        } else if (obj instanceof VolumeStoragePoolMap) {
            sendChange(StoragePool.class, accountId, ((VolumeStoragePoolMap) obj).getStoragePoolId(), schedule, context);
            sendChange(Volume.class, accountId, ((VolumeStoragePoolMap) obj).getVolumeId(), schedule, context);
        } else if (obj instanceof Mount) {
            sendChange(Instance.class, accountId, ((Mount) obj).getInstanceId(), schedule, context);
            sendChange(Volume.class, accountId, ((Mount) obj).getVolumeId(), schedule, context);
        }
    }

    protected Object sendChange(Class<?> type, Object accountId, Long id, boolean schedule, ProcessServiceContext context) {
        if (id == null) {
            return null;
        }


        String typeName = objectManager.getType(type);
        if (accountId == null) {
            accountId = ObjectUtils.getAccountId(objectManager.loadResource(typeName, id));
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put(ObjectMetaDataManager.ACCOUNT_FIELD, accountId);
        Event event = makeEvent(typeName, id.toString()).withData(data);
        publish(schedule, event, context);

        return accountId;
    }

}
