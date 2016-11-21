package io.cattle.platform.process.host;

import io.cattle.platform.async.utils.ResourceTimeoutException;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.MachineConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.PhysicalHost;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.object.util.TransitioningUtils;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.util.exception.ExecutionException;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class HostProvision extends AbstractDefaultProcessHandler {

    @Inject
    ResourceMonitor resourceMonitor;
    @Inject
    EventService eventService;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        final Host host = (Host)state.getResource();
        PhysicalHost physicalHost = objectManager.loadResource(PhysicalHost.class, host.getPhysicalHostId());
        if (physicalHost == null) {
            return null;
        }

        physicalHost = resourceMonitor.waitFor(physicalHost, 5 * 60000, new ResourcePredicate<PhysicalHost>() {
            @Override
            public boolean evaluate(final PhysicalHost obj) {
                boolean transitioning = objectMetaDataManager.isTransitioningState(obj.getClass(),
                        obj.getState());
                if (!transitioning) {
                    return true;
                }

                objectManager.reload(host);
                if (!host.getState().equals(HostConstants.STATE_PROVISIONING)) {
                    throw new ResourceTimeoutException(host, "provisioning canceled");
                }

                String message = TransitioningUtils.getTransitioningMessage(obj);
                objectManager.setFields(host, ObjectMetaDataManager.TRANSITIONING_MESSAGE_FIELD, message);
                DeferredUtils.nest(new Runnable() {
                    @Override
                    public void run() {
                        ObjectUtils.publishChanged(eventService, objectManager, host);
                    }
                });

                return false;
            }

            @Override
            public String getMessage() {
                return "machine to provision";
            }
        });

        if (!CommonStatesConstants.ACTIVE.equals(physicalHost.getState())) {
            objectProcessManager.scheduleStandardProcess(StandardProcess.ERROR, host, null);
            String message = TransitioningUtils.getTransitioningMessage(physicalHost);
            ExecutionException e = new ExecutionException(message);
            e.setResources(host);
            throw e;
        }

        resourceMonitor.waitFor(host, 5 * 60000, new ResourcePredicate<Host>() {
            @Override
            public boolean evaluate(Host obj) {
                if (!host.getState().equals(HostConstants.STATE_PROVISIONING)) {
                    throw new ResourceTimeoutException(host, "provisioning canceled");
                }

                return obj.getAgentId() != null;
            }

            @Override
            public String getMessage() {
                return "agent to check in";
            }
        });

        return new HandlerResult(
                MachineConstants.FIELD_DRIVER, physicalHost.getDriver()
                ).withChainProcessName(objectProcessManager.getProcessName(host, StandardProcess.ACTIVATE));
    }

}
