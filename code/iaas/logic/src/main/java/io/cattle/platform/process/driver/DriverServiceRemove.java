package io.cattle.platform.process.driver;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.AbstractProcessLogic;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class DriverServiceRemove extends AbstractProcessLogic implements ProcessPreListener {

    @Inject
    ObjectManager objectManager;
    @Inject
    ObjectProcessManager processManager;

    @Override
    public String[] getProcessNames() {
        return new String[]{"service.remove"};
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        for (String driverKey : DriverServiceActivate.DRIVERS) {
            Class<?> driverClass = objectManager.getSchemaFactory().getSchemaClass(driverKey);

            Service service = (Service)state.getResource();
            for (Object driver : objectManager.children(service, driverClass)) {
                processManager.scheduleStandardProcess(StandardProcess.REMOVE, driver, null);
            }
        }
        return null;
    }

}