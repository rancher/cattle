package io.cattle.platform.inator.process;

import io.cattle.platform.core.constants.ExternalEventConstants;
import io.cattle.platform.core.constants.GenericObjectConstants;
import io.cattle.platform.core.constants.LabelConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.inator.InatorLifecycleManager;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ServiceReconcileTrigger extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    @Inject
    InatorLifecycleManager lifecycleManager;

    @Override
    public String[] getProcessNames() {
        return new String[] {
                LabelConstants.PROCESS_HOSTLABELMAP_CREATE,
                LabelConstants.PROCESS_HOSTLABELMAP_REMOVE,
                ExternalEventConstants.PROCESS_EXTERNAL_EVENT_CREATE,
                ServiceConstants.PROCESS_SERVICE_UPDATE,
                GenericObjectConstants.PROCESS_CREATE,
                "agent.*",
                "host.*",
                "deploymentunit.*",
        };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        lifecycleManager.triggerServiceUpdate(state.getResource());
        return null;
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }

}
