package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

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
        ObjectUtils.publishChanged(eventService, objectManager, service);
        return null;
    }

}
