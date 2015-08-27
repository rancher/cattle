package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants.KIND;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.util.type.Priority;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class LoadBalancerServiceUpdate extends AbstractObjectProcessHandler implements ProcessPreListener,
        Priority {

    @Inject
    ServiceDiscoveryService sdService;
    @Inject
    JsonMapper jsonMapper;

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceDiscoveryConstants.PROCESS_SERVICE_UPDATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Service service = (Service) state.getResource();
        if (service.getKind().equalsIgnoreCase(KIND.LOADBALANCERSERVICE.name())) {
            DataAccessor certIdsObj = DataAccessor.fromMap(state.getData())
                    .withKey(LoadBalancerConstants.FIELD_LB_CERTIFICATE_IDS);
            DataAccessor defaultCertIdObj = DataAccessor.fromMap(state.getData())
                    .withKey(LoadBalancerConstants.FIELD_LB_DEFAULT_CERTIFICATE_ID);
            if (certIdsObj == null && defaultCertIdObj == null) {
                return null;
            }

            List<? extends Long> certIds = certIdsObj.asList(jsonMapper, Long.class);
            Long defaultCertId = defaultCertIdObj.as(Long.class);

            sdService.updateLoadBalancerService(service, certIds, defaultCertId);

        }
        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
