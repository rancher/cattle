package io.cattle.platform.process.common.handler;

import io.cattle.platform.engine.process.ProcessRouter;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.serialization.ObjectSerializer;

public class ExternalHandlerFactory {

    ProcessRouter processRouter;
    EventService eventService;
    ObjectProcessManager processManager;
    ObjectManager objectManager;
    ObjectMetaDataManager metaDataManager;
    ObjectSerializer objectSerializer;

    public ExternalHandlerFactory(ProcessRouter processRouter, EventService eventService, ObjectProcessManager processManager, ObjectManager objectManager, ObjectMetaDataManager metaDataManager, ObjectSerializer objectSerializer) {
        this.processRouter = processRouter;
        this.eventService = eventService;
        this.processManager = processManager;
        this.objectManager = objectManager;
        this.metaDataManager = metaDataManager;
        this.objectSerializer = objectSerializer;
    }

    public ExternalProcessHandler handler(String name) {
        return new ExternalProcessHandler(name, eventService, objectManager, processManager, metaDataManager, objectSerializer);
    }

    public ExternalProcessHandler handler(String name, String condition) {
        ExternalProcessHandler handler = new ExternalProcessHandler(name, eventService, objectManager, processManager, metaDataManager, objectSerializer);
        handler.setCondition(condition);
        return handler;
    }

}
