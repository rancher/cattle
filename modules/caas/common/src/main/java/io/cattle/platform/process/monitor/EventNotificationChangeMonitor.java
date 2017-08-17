package io.cattle.platform.process.monitor;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Mount;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.engine.manager.impl.ProcessRecord;
import io.cattle.platform.engine.process.ProcessServiceContext;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.StateChangeMonitor;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.framework.event.FrameworkEvents;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.Map;

public class EventNotificationChangeMonitor implements StateChangeMonitor {

    ObjectManager objectManager;

    public EventNotificationChangeMonitor(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    @Override
    public void onChange(boolean schedule, String previousState, String newState, ProcessRecord record, ProcessState state, ProcessServiceContext context) {
        Map<String, Object> data = CollectionUtils.asMap(
                "previousState", previousState,
                "state", newState);
        addData(data, schedule, state, context);

        Event event = makeEvent(record.getResourceType(), record.getResourceId(), data);
        publish(schedule, event, context);
    }

    protected EventVO<Object, Object> makeEvent(String resourceType, String resourceId, Object data) {
        return EventVO.newEvent(FrameworkEvents.STATE_CHANGE)
                .withResourceType(resourceType)
                .withResourceId(resourceId)
                .withData(data);
    }

    protected void publish(boolean schedule, Event event, ProcessServiceContext context) {
        if (schedule) {
            DeferredUtils.deferPublish(context.getEventService(), event);
        } else {
            context.getEventService().publish(event);
        }
    }

    public void addData(Map<String, Object> data, boolean schedule, ProcessState state, ProcessServiceContext context) {
        Object resource = state.getResource();
        Object accountId = ObjectUtils.getAccountId(resource);
        Object clusterId = ObjectUtils.getClusterId(resource);

        data.put(ObjectMetaDataManager.ACCOUNT_FIELD, accountId);
        data.put(ObjectMetaDataManager.CLUSTER_FIELD, clusterId);

        Object obj = state.getResource();
        if (obj instanceof Service) {
            sendChange(Stack.class, accountId, clusterId, ((Service) obj).getStackId(), schedule, context);
        } else if (obj instanceof Mount) {
            sendChange(Instance.class, accountId, clusterId, ((Mount) obj).getInstanceId(), schedule, context);
            sendChange(Volume.class, accountId, clusterId, ((Mount) obj).getVolumeId(), schedule, context);
        }
    }

    protected void sendChange(Class<?> type, Object accountId, Object clusterId, Long id, boolean schedule, ProcessServiceContext context) {
        if (id == null) {
            return;
        }

        String typeName = objectManager.getType(type);
        Object data = CollectionUtils.asMap(
            ObjectMetaDataManager.ACCOUNT_FIELD, accountId,
            ObjectMetaDataManager.CLUSTER_FIELD, clusterId);

        Event event = makeEvent(typeName, id.toString(), data);
        publish(schedule, event, context);
    }

}
