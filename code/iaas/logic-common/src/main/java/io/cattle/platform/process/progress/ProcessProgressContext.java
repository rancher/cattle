package io.cattle.platform.process.progress;

import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.framework.event.FrameworkEvents;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.util.type.CollectionUtils;

import javax.inject.Inject;

public class ProcessProgressContext implements ProcessProgressInstance {

    ProcessState processState;
    ProcessProgressState progressState;
    ObjectManager objectManager;
    JsonMapper jsonMapper;
    EventService eventService;

    public ProcessProgressContext(ProcessState processState, ObjectManager objectManager, JsonMapper jsonMapper, EventService eventService) {
        super();
        this.processState = processState;
        this.objectManager = objectManager;
        this.jsonMapper = jsonMapper;
        this.eventService = eventService;
    }

    @Override
    public void init(ProcessState state, int... checkpointWeights) {
        DataAccessor value = DataAccessor.fromMap(state.getData()).withKey("progress");

        progressState = value.as(jsonMapper, ProcessProgressState.class);

        if (progressState == null) {
            progressState = new ProcessProgressState(checkpointWeights);
            value.set(progressState);
        }
    }

    @Override
    public void checkPoint(String name) {
        if (progressState.checkPoint(name)) {
            update();
        }
    }

    @Override
    public void progress(Integer progress) {
        if (progressState.setIntermediateProgress(progress)) {
            update();
        }
    }

    @Override
    public void messsage(String message) {
        if (progressState.setMessage(message)) {
            update();
        }
    }

    protected void update() {
        String message = progressState.getMessage();
        Integer progress = progressState.getProgress();

        final Object resource = objectManager.reload(processState.getResource());

        objectManager.setFields(resource, ObjectMetaDataManager.TRANSITIONING_MESSAGE_FIELD, message, ObjectMetaDataManager.TRANSITIONING_PROGRESS_FIELD,
                progress);

        DeferredUtils.nest(new Runnable() {
            @Override
            public void run() {
                String type = objectManager.getType(resource);
                Object id = ObjectUtils.getId(resource);
                Object accountId = ObjectUtils.getAccountId(resource);
                if (id != null) {
                    Event event = EventVO.newEvent(FrameworkEvents.RESOURCE_PROGRESS).withResourceType(type).withResourceId(id.toString()).withData(
                            CollectionUtils.asMap(ObjectMetaDataManager.ACCOUNT_FIELD, accountId));

                    eventService.publish(event);
                }
            }
        });
    }

    @Override
    public String getCurrentCheckpoint() {
        return progressState.getCurrentCheckpoint();
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public EventService getEventService() {
        return eventService;
    }

    @Inject
    public void setEventService(EventService eventService) {
        this.eventService = eventService;
    }

}
