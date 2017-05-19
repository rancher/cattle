package io.cattle.platform.process.service;

import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import javax.inject.Inject;
import javax.inject.Named;


@Named
public class ServiceFinishupgrade extends AbstractDefaultProcessHandler {

    @Inject
    ServiceDao svcDao;
    @Inject
    ObjectProcessManager objectProcessManager;


    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Service service = (Service)state.getResource();

        for (Instance instance : svcDao.getInstancesToGarbageCollect(service)) {
            objectProcessManager.stopAndRemove(instance, null);
        }

        return null;
    }

}
