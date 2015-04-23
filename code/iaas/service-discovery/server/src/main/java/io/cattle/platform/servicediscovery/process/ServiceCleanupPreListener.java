package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants.KIND;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.util.type.Priority;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * this handler takes care of
 * a) removing service-instance mappings referencing instances in Removed state
 * b) scaling down the service to the requested scale
 * 
 * the handler gets invoked on all processes associated with the service, so at the moment of process execution the
 * server is in "clean" state
 */
@Named
public class ServiceCleanupPreListener extends AbstractObjectProcessLogic implements ProcessPreListener,
        Priority {

    @Inject
    ServiceExposeMapDao exposeMapDao;

    @Inject
    JsonMapper jsonMapper;

    @Inject
    ServiceDiscoveryService sdService;

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceDiscoveryConstants.PROCESS_SERVICE_ACTIVATE,
                ServiceDiscoveryConstants.PROCESS_SERVICE_UPDATE, ServiceDiscoveryConstants.PROCESS_SERVICE_DEACTIVATE,
                ServiceDiscoveryConstants.PROCESS_SERVICE_REMOVE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Service service = (Service)state.getResource();
        // 1) remove destroyed instances maps
        List<? extends ServiceExposeMap> maps = exposeMapDao.listServiceRemovedInstancesMaps(service.getId());
        for (ServiceExposeMap map : maps) {
            objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, map, null);
        }

        int requestedScale = DataAccessor.field(service, ServiceDiscoveryConstants.FIELD_SCALE,
                jsonMapper,
                Integer.class);

        // 2) scale down the service
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
