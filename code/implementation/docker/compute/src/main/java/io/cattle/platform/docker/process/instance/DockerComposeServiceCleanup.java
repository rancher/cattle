package io.cattle.platform.docker.process.instance;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.docker.service.ComposeManager;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import javax.inject.Inject;

public class DockerComposeServiceCleanup extends AbstractObjectProcessLogic implements ProcessPreListener {

    @Inject
    ComposeManager composeManager;

    @Override
    public String[] getProcessNames() {
        return new String[] {"service.remove", "serviceexposemap.remove"};
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Object obj = state.getResource();
        if (obj instanceof Service) {
            composeManager.cleanupResources((Service)obj);
        } else if (obj instanceof ServiceExposeMap) {
            ServiceExposeMap map = (ServiceExposeMap)obj;
            Service service = objectManager.loadResource(Service.class, map.getServiceId());
            composeManager.cleanupResources(service);
        }
        return null;
    }

}