package io.cattle.platform.agent.instance.link.process;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.core.constants.InstanceLinkConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceLink;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;

import javax.inject.Inject;

import com.netflix.config.DynamicLongProperty;

public class AgentInstanceLinkActivate extends AbstractObjectProcessHandler {

    private static final DynamicLongProperty WAIT_TIME = ArchaiusUtil.getLong("instance.link.target.wait.time.millis");

    @Inject
    ResourceMonitor resourceMonitor;

    @Override
    public String[] getProcessNames() {
        return new String[] { "instancelink.activate" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        InstanceLink link = (InstanceLink) state.getResource();
        Instance instance = loadResource(Instance.class, link.getInstanceId());
        Instance targetInstance = loadResource(Instance.class, link.getTargetInstanceId());

        if (instance == null) {
            return null;
        }

        long timeout = DataAccessor.fromDataFieldOf(instance).withKey(InstanceLinkConstants.DATA_LINK_WAIT_TIME).withDefault(WAIT_TIME.get()).as(Long.class);

        try {
            targetInstance = resourceMonitor.waitFor(targetInstance, timeout, new ResourcePredicate<Instance>() {
                @Override
                public boolean evaluate(Instance obj) {
                    return obj.getFirstRunning() != null;
                }

                @Override
                public String getMessage() {
                    return "running";
                }
            });
        } catch (TimeoutException e) {
            /* We are going to ignore this now and just continue forward with no ports.  This is really only needed for the
             * env vars which are not commonly used
             */
        }

        return new HandlerResult().withShouldContinue(true);
    }
}
