package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.api.dao.ServiceDao;
import io.cattle.platform.util.type.Priority;

import javax.inject.Inject;

public class InstanceMetadataUpdateTrigger extends AbstractObjectProcessLogic implements ProcessPreListener, Priority {

    @Inject
    ServiceDao serviceDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { "nic.activate" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Nic nic = (Nic) state.getResource();
        Instance instance = objectManager.loadResource(Instance.class, nic.getInstanceId());
        serviceDao.incrementMetadataRevision(instance.getAccountId(), instance);
        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
