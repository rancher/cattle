package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.util.type.CollectionUtils;
import io.cattle.platform.util.type.Priority;

import java.util.List;

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

    @Inject
    ServiceExposeMapDao exposeMapDao;

    @Inject
    GenericMapDao mapDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceDiscoveryConstants.PROCESS_SERVICE_UPDATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Service service = (Service) state.getResource();
        int scale = DataAccessor.field(service, ServiceDiscoveryConstants.FIELD_SCALE,
                jsonMapper,
                Integer.class);
        // on scale up, skip
        List<? extends Instance> serviceInstances = svcExposeDao.listNonRemovedInstancesForService(service.getId());
        if (serviceInstances.size() <= scale) {
            return null;
        }
        // remove instances
        int toRemove = serviceInstances.size() - scale;
        for (int i = serviceInstances.size() - toRemove; i < serviceInstances.size(); i++) {
            String instanceName = sdService.getInstanceName(service, i);
            Instance instance = exposeMapDao.getServiceInstance(service.getId(), instanceName);
            if (instance != null) {
                removeInstance(instance);
            }
        }
        return null;
    }
    
    private void removeInstance(Instance instance) {
        try {
            objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, instance, null);
        } catch (ProcessCancelException e) {
            objectProcessManager.scheduleProcessInstance(InstanceConstants.PROCESS_STOP, instance,
                    CollectionUtils.asMap(InstanceConstants.REMOVE_OPTION, true));
        }
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
