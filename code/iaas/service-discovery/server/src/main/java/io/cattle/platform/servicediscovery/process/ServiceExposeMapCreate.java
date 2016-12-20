package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.framework.event.FrameworkEvents;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ServiceExposeMapCreate extends AbstractObjectProcessHandler {
    @Inject
    ServiceDiscoveryService sdService;
    @Inject
    JsonMapper jsonMapper;

    @Inject
    NetworkDao ntwkDao;

    @Inject
    EventService eventService;

    @Override
    public String[] getProcessNames() {
        return new String[] { "serviceexposemap.create" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        ServiceExposeMap exposeMap = (ServiceExposeMap) state.getResource();
        if (exposeMap.getManaged()) {
            return null;
        }
        Service service = objectManager.loadResource(Service.class, exposeMap.getServiceId());
        publishEvent(service);
        return null;
    }

    protected void publishEvent(Service service) {
        Map<String, Object> data = new HashMap<>();
        data.put(ObjectMetaDataManager.ACCOUNT_FIELD, service.getAccountId());

        Event event = EventVO.newEvent(FrameworkEvents.STATE_CHANGE)
                .withData(data)
                .withResourceType(service.getKind())
                .withResourceId(service.getId().toString());

        eventService.publish(event);
    }
}
