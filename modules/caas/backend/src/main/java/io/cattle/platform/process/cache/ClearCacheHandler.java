package io.cattle.platform.process.cache;

import io.cattle.platform.core.cache.DBCacheManager;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.framework.event.FrameworkEvents;

public class ClearCacheHandler implements ProcessHandler {

    EventService eventService;
    DBCacheManager cacheManager;

    public ClearCacheHandler(EventService eventService, DBCacheManager cacheManager) {
        super();
        this.eventService = eventService;
        this.cacheManager = cacheManager;
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        cacheManager.clear();
        DeferredUtils.defer(new Runnable() {
            @Override
            public void run() {
                eventService.publish(EventVO.newEvent(FrameworkEvents.CLEAR_CACHE));
                cacheManager.clear();
            }
        });
        return null;
    }

}