package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.dao.DynamicSchemaDao;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;

import javax.inject.Inject;

public class ServiceRemovePostListener extends AbstractObjectProcessLogic implements ProcessPostListener {

    @Inject
    DynamicSchemaDao dynamicSchemaDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceDiscoveryConstants.PROCESS_SERVICE_REMOVE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Service service = (Service)state.getResource();
        dynamicSchemaDao.deleteSchemas(service.getId());
        return null;
    }

}