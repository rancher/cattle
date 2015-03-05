package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.process.progress.ProcessProgress;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.netflix.config.DynamicLongProperty;

@Named
public class EnvironmentRemove extends AbstractObjectProcessHandler {

    private static final DynamicLongProperty TIMEOUT = ArchaiusUtil.getLong("service.remove.wait.time.millis");

    @Inject
    ResourceMonitor resourceMonitor;

    @Inject
    ProcessProgress progress;

    @Inject
    ServiceDiscoveryService sdServer;

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceDiscoveryConstants.PROCESS_ENV_REMOVE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Environment env = (Environment) state.getResource();
        List<? extends Service> services = objectManager.mappedChildren(env, Service.class);
        if (!services.isEmpty()) {
            progress.init(state, sdServer.getWeights(services.size(), 100));
            removeServices(services);
        }
        return null;
    }

    private void removeServices(List<? extends Service> services) {
        for (Service service : services) {
            try {
                objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE,
                        service, null);
            } catch (ProcessCancelException e) {
                // do nothing
            }
        }
        for (Service service : services) {
            progress.checkPoint("remove service " + service.getName());
            service = resourceMonitor.waitFor(service, TIMEOUT.get(), new ResourcePredicate<Service>() {
                @Override
                public boolean evaluate(Service obj) {
                    return CommonStatesConstants.REMOVED.equals(obj.getState());
                }
            });
        }
    }
}
