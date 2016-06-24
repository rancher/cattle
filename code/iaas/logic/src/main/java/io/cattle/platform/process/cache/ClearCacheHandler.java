package io.cattle.platform.process.cache;

import io.cattle.platform.core.cache.DBCacheManager;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.engine.handler.AbstractProcessLogic;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.iaas.event.IaasEvents;
import io.cattle.platform.util.type.Priority;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ClearCacheHandler extends AbstractProcessLogic implements ProcessPostListener, Priority {

    @Inject
    EventService eventService;
    
    @Inject
    DBCacheManager cacheManager;

    @Override
    public String[] getProcessNames() {
        return new String[] {
                "externalhandler.*",
                "externalhandlerexternalhandlerprocessmap.*",
                "externalhandlerprocess.*",
                "externalhandler.*",
                "dynamicschema.*"
        };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        cacheManager.clear();
        DeferredUtils.defer(new Runnable() {
            @Override
            public void run() {
                eventService.publish(EventVO.newEvent(IaasEvents.CLEAR_CACHE));
                cacheManager.clear();
            }
        });
        return null;
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }

}