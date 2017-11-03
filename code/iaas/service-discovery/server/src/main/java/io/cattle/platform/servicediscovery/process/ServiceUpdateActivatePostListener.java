package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.core.constants.ServiceConstants;

import java.util.Date;

import javax.inject.Named;

@Named
public class ServiceUpdateActivatePostListener extends AbstractObjectProcessLogic implements ProcessPostListener,
        Priority {

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Service service = (Service) state.getResource();

        DataAccessor.fields(service).withKey(ServiceConstants.LAST_ACTIVE).set(new Date().getTime());
        objectManager.persist(service);

        return null;
    }

    @Override
    public String[] getProcessNames() {
        return new String[] { "service.activate", "service.update" };
    }

    @Override
    public int getPriority() {
        return Priority.BIG_ONE;
    }

}
