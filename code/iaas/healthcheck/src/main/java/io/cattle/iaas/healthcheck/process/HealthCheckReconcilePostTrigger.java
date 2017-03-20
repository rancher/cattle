package io.cattle.iaas.healthcheck.process;

import io.cattle.iaas.healthcheck.service.HealthcheckInstancesLookup;
import io.cattle.iaas.healthcheck.service.HealthcheckService;
import io.cattle.iaas.healthcheck.service.HealthcheckService.HealthcheckInstanceType;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.api.dao.ServiceDao;
import io.cattle.platform.util.type.Priority;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class HealthCheckReconcilePostTrigger extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {
    @Inject
    ServiceDao serviceDao;

    @Inject
    HealthcheckService healthcheckService;

    @Inject
    HostDao hostDao;

    @Inject
    NetworkDao ntwkDao;

    @Inject
    List<HealthcheckInstancesLookup> instancesLookups;

    @Override
    public String[] getProcessNames() {
        return new String[] { AgentConstants.PROCESS_RECONNECT, AgentConstants.PROCESS_FINISH_RECONNECT,
                "networkserviceproviderinstancemap.create",
                "healthcheckinstancehostmap.remove" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        List<Instance> instances = new ArrayList<>();
        for (HealthcheckInstancesLookup lookup : instancesLookups) {
            List<? extends Instance> i = lookup.getInstances(state.getResource());
            if (i != null) {
                instances.addAll(i);
            }
        }

        reregisterInstancesForHealtchecks(instances);

        return null;
    }

    protected void reregisterInstancesForHealtchecks(List<? extends Instance> instances) {
        for (Instance instance : instances) {
            healthcheckService.registerForHealtcheck(HealthcheckInstanceType.INSTANCE, instance.getId());
        }
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }
}