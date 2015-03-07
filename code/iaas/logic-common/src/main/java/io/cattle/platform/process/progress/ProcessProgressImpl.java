package io.cattle.platform.process.progress;

import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;

import javax.inject.Inject;

import org.apache.cloudstack.managed.threadlocal.ManagedThreadLocal;

public class ProcessProgressImpl implements ProcessProgress {

    private static final ManagedThreadLocal<ProcessProgressContext> TL = new ManagedThreadLocal<ProcessProgressContext>();

    ObjectManager objectManager;
    JsonMapper jsonMapper;
    EventService eventService;

    @Override
    public ProcessProgressInstance get() {
        return TL.get();
    }

    @Override
    public void init(ProcessState state, int... checkpointWeights) {
        ProcessProgressContext context = TL.get();

        if (context != null) {
            return;
        }

        context = new ProcessProgressContext(state, objectManager, jsonMapper, eventService);
        context.init(state, checkpointWeights);

        TL.set(context);
    }

    @Override
    public void checkPoint(String name) {
        ProcessProgressContext context = TL.get();

        if (context == null) {
            return;
        }

        context.checkPoint(name);
    }

    @Override
    public void progress(Integer progress) {
        ProcessProgressContext context = TL.get();

        if (context == null) {
            return;
        }

        context.progress(progress);
    }

    @Override
    public String getCurrentCheckpoint() {
        ProcessProgressContext context = TL.get();

        if (context == null) {
            return null;
        }

        return context.getCurrentCheckpoint();
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