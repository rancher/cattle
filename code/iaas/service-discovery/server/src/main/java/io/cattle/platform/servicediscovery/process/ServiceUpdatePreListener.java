package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.dao.impl.InstanceDaoImpl.IpAddressToServiceIndex;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.resource.pool.PooledResourceOptions;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.util.type.CollectionUtils;
import io.cattle.platform.util.type.Priority;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

/*
 * This code takes care of updating retainIp address field
 */
@Named
public class ServiceUpdatePreListener extends AbstractObjectProcessLogic implements ProcessPreListener,
        Priority {

    @Inject
    ServiceDiscoveryService sdService;
    @Inject
    InstanceDao instanceDao;
    @Inject
    ResourcePoolManager poolManager;
    @Inject
    NetworkDao ntwkDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceConstants.PROCESS_SERVICE_UPDATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Service service = (Service) state.getResource();
        Map<String, Object> data = state.getData();
        Boolean oldRetainIp = false;
        if (data.get("old") != null) {
            Map<String, Object> old = CollectionUtils.toMap(data.get("old"));
            if (old.get(ServiceConstants.FIELD_SERVICE_RETAIN_IP) != null) {
                oldRetainIp = (Boolean) old.get(ServiceConstants.FIELD_SERVICE_RETAIN_IP);
            }
        }
        Boolean newRetainIp = DataAccessor.fieldBoolean(service, ServiceConstants.FIELD_SERVICE_RETAIN_IP);

        boolean update = false;
        if (newRetainIp.booleanValue() && !oldRetainIp.booleanValue()) {
            update = true;
        }
        if (!update) {
            return null;
        }

        // get all service managed instances and their ips/indexes
        List<IpAddressToServiceIndex> ipToIndex = instanceDao.getIpToIndex(service);
        for (IpAddressToServiceIndex i : ipToIndex) {
            // transfer the resource
            PooledResourceOptions options = new PooledResourceOptions();
            options.setRequestedItem(i.getIpAddress().getAddress());
            poolManager.transferResource(i.getSubnet(), i.getIpAddress(), i.getIndex(), options);
            // allocate to service index
            sdService.setServiceIndexIp(i.getIndex(), i.getIpAddress().getAddress());
        }

        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
