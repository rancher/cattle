package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants.KIND;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.util.type.Priority;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ServiceUpdatePostListener extends AbstractObjectProcessLogic implements ProcessPostListener,
        Priority {

    @Inject
    ServiceExposeMapDao svcExposeDao;

    @Inject
    JsonMapper jsonMapper;

    @Inject
    ServiceDiscoveryService sdService;


    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceDiscoveryConstants.PROCESS_SERVICE_UPDATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Service service = (Service) state.getResource();
        int requestedScale = DataAccessor.field(service, ServiceDiscoveryConstants.FIELD_SCALE,
                jsonMapper,
                Integer.class);

        if (service.getKind().equalsIgnoreCase(KIND.SERVICE.name())) {
            sdService.scaleDownService(service, requestedScale);
        } else if (service.getKind().equalsIgnoreCase(KIND.LOADBALANCERSERVICE.name())) {
            sdService.scaleDownLoadBalancerService(service, requestedScale);
        }
        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
