package io.cattle.platform.process.progress;

import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;

import org.apache.cloudstack.managed.threadlocal.ManagedThreadLocal;

public class ProcessProgressImpl implements ProcessProgress {

    private static final ManagedThreadLocal<ProcessProgressContext> TL = new ManagedThreadLocal<>();

    ObjectManager objectManager;
    JsonMapper jsonMapper;
    EventService eventService;

    public ProcessProgressImpl(ObjectManager objectManager, JsonMapper jsonMapper, EventService eventService) {
        super();
        this.objectManager = objectManager;
        this.jsonMapper = jsonMapper;
        this.eventService = eventService;
    }

    @Override
    public ProcessProgressInstance get() {
        return TL.get();
    }

    @Override
    public void init(ProcessState state) {
        ProcessProgressContext context = TL.get();

        if (context != null) {
            return;
        }

        context = new ProcessProgressContext(state, objectManager, jsonMapper, eventService);
        context.init(state);

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

}