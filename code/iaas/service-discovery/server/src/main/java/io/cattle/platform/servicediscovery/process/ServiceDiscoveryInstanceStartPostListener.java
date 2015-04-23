package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.SERVICE_EXPOSE_MAP;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.lb.instance.service.LoadBalancerInstanceManager;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.util.type.Priority;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * this handler registers service discovery instance in service_expose maps
 *
 */

@Named
public class ServiceDiscoveryInstanceStartPostListener extends AbstractObjectProcessLogic implements
        ProcessPostListener,
        Priority {

    @Inject
    GenericMapDao mapDao;

    @Inject
    GenericResourceDao resourceDao;

    @Inject
    LoadBalancerInstanceManager lbInstanceService;

    @Override
    public String[] getProcessNames() {
        return new String[] { InstanceConstants.PROCESS_START };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance) state.getResource();
        Long serviceId = null;
        if (lbInstanceService.isLbInstance(instance)) {
            LoadBalancer lb = lbInstanceService.getLoadBalancerForInstance(instance);
            serviceId = lb.getServiceId();
        } else {
            Integer requestedServiceId = (Integer)DataUtils.getFields(instance).get(
                    ServiceDiscoveryConstants.FIELD_SERVICE_ID);
            if (requestedServiceId != null) {
                serviceId = requestedServiceId.longValue();
            }
        }

        if (serviceId == null) {
            return null;
        }

        ServiceExposeMap instanceServiceMap = mapDao.findNonRemoved(ServiceExposeMap.class, Instance.class,
                instance.getId(),
                Service.class, serviceId);

        if (instanceServiceMap == null) {
            instanceServiceMap = resourceDao.createAndSchedule(ServiceExposeMap.class, SERVICE_EXPOSE_MAP.INSTANCE_ID,
                    instance.getId(), SERVICE_EXPOSE_MAP.SERVICE_ID, serviceId);
        }

        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT_OVERRIDE;
    }

}
