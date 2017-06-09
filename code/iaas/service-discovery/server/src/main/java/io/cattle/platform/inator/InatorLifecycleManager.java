package io.cattle.platform.inator;

import io.cattle.platform.activity.ActivityService;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.process.impl.ProcessDelayException;
import io.cattle.platform.eventing.annotation.AnnotatedEventListener;
import io.cattle.platform.inator.Unit.UnitState;
import io.cattle.platform.inator.unit.InstanceUnit;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.servicediscovery.deployment.lookups.DeploymentUnitLookup;
import io.cattle.platform.servicediscovery.service.lookups.ServiceLookup;
import io.cattle.platform.util.exception.ExecutionException;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

@Named
public class InatorLifecycleManager implements AnnotatedEventListener {

    public static final String RECONCILE = "reconcile";
    public static final String DU_RECONCILE = "deployment-unit-update";

    @Inject
    ObjectManager objectManager;
    @Inject
    Deployinator deployinator;
    @Inject
    ObjectProcessManager processManager;
    @Inject
    List<ServiceLookup> serviceLookups;
    @Inject
    List<DeploymentUnitLookup> deploymentUnitLookups;
    @Inject
    ActivityService activityService;
    @Inject
    ScheduledExecutorService executorService;

    public void handleProcess(String process, Object resource, Long resourceId) {
        Long accountId = null;
        Long serviceId = null;
        Long deploymentUnitId = null;
        Class<?> clz;
        if (resource instanceof Service) {
            accountId = ((Service) resource).getAccountId();
            serviceId = ((Service) resource).getId();
            clz = Service.class;
        } else if (resource instanceof DeploymentUnit) {
            accountId = ((DeploymentUnit) resource).getAccountId();
            serviceId = ((DeploymentUnit) resource).getServiceId();
            deploymentUnitId = ((DeploymentUnit) resource).getId();
            clz = DeploymentUnit.class;
        } else {
            return;
        }

        activityService.run(accountId, serviceId, deploymentUnitId, process, "Processing", () -> {
            Result result = reconcile(clz, resourceId);
            if (result.isGood()) {
                return;
            }

            if (result.getState() == UnitState.ERROR) {
                throw new ExecutionException("Reconciling returned ERROR: " + result.getReason(), resource);
            }

            throw new ProcessDelayException(new Date(System.currentTimeMillis() + 120000L));
        });
    }

    protected Result reconcile(Class<?> clz, Long resourceId) {
        Result result = deployinator.reconcile(clz, resourceId);
        if (result.getState() == UnitState.ERROR) {
            processManager.error(objectManager.loadResource(clz, resourceId), null);
        }

        logResult(result, new HashSet<>());
        if (result.getState() == UnitState.WAITING) {
            activityService.waiting();
        }

        activityService.info("Result: %s %s", result.getState(), result.getUnit() == null ? "" : result.getUnit().getDisplayName());
        return result;
    }

    protected void logResult(Result result, Set<Result> seen) {
        if (result.isGood()) {
            return;
        }

        if (seen.contains(result)) {
            return;
        }
        seen.add(result);

        if (StringUtils.isNotBlank(result.getReason())) {
            Unit unit = result.getUnit();
            if (unit instanceof InstanceUnit) {
                activityService.instance(((InstanceUnit) unit).getInstanceId(),
                        result.getState().toString().toLowerCase(),
                        result.getReason(), result.getLogLevel());
            } else if ("error".equals(result.getLogLevel())) {
                activityService.error(result.getReason());
            } else {
                activityService.info(result.getReason());
            }
        }

        for (Result subResult : result.getResults()) {
            logResult(subResult, seen);
        }
    }

    public Set<Long> impactedServices(Object resource) {
        Set<Long> services = new HashSet<>();
        for (ServiceLookup lookup : serviceLookups) {
            Collection<? extends Service> lookupSvs = lookup.getServices(resource);
            if (lookupSvs != null) {
                for (Service service : lookupSvs) {
                    services.add(service.getId());
                }
            }
        }

        return services;
    }

    public Set<Long> impacetedDeploymentUnitUpdate(Object resource) {
        Set<Long> deploymentUnitIds = new HashSet<>();
        for (DeploymentUnitLookup lookup : deploymentUnitLookups) {
            Collection<? extends DeploymentUnit> deploymentUnits = lookup.getDeploymentUnits(resource);
            if (deploymentUnits != null) {
                for (DeploymentUnit unit : deploymentUnits) {
                    deploymentUnitIds.add(unit.getId());
                }
            }
        }

        return deploymentUnitIds;
    }

}
