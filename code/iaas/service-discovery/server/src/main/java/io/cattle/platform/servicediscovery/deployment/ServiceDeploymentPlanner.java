package io.cattle.platform.servicediscovery.deployment;

import static io.cattle.platform.core.model.tables.DeploymentUnitTable.*;
import io.cattle.platform.activity.ActivityLog;
import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.addon.InstanceHealthCheck.Strategy;
import io.cattle.platform.core.addon.RecreateOnQuorumStrategyConfig;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.engine.process.ExitReason;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.engine.process.impl.ProcessExecutionExitException;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.TransitioningUtils;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentManagerContext;
import io.cattle.platform.servicediscovery.deployment.impl.healthaction.HealthCheckActionHandler;
import io.cattle.platform.servicediscovery.deployment.impl.healthaction.NoopHealthCheckActionHandler;
import io.cattle.platform.servicediscovery.deployment.impl.healthaction.RecreateHealthCheckActionHandler;
import io.cattle.platform.servicediscovery.deployment.impl.healthaction.RecreateOnQuorumHealthCheckActionHandler;
import io.cattle.platform.servicediscovery.deployment.impl.unit.DeploymentUnitInstanceIdGeneratorImpl;
import io.cattle.platform.util.exception.DeploymentUnitAllocateException;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * This class creates new deploymentUnits based on the service requirements (scale/global)
 *
 */
public abstract class ServiceDeploymentPlanner {

    protected Service service;
    protected Stack stack;
    private Map<String, DeploymentUnit> healthyUnits = new HashMap<>();
    private Map<String, DeploymentUnit> unhealthyUnits = new HashMap<>();
    private Map<String, DeploymentUnit> badUnits = new HashMap<>();
    private Map<String, DeploymentUnit> allUnits = new HashMap<>();
    protected DeploymentManagerContext context;
    protected HealthCheckActionHandler healthActionHandler = new RecreateHealthCheckActionHandler();
    private static final Set<String> ERROR_STATES = new HashSet<String>(Arrays.asList(
            InstanceConstants.STATE_ERRORING,
            InstanceConstants.STATE_ERROR,
            CommonStatesConstants.REMOVED,
            CommonStatesConstants.REMOVING,
            CommonStatesConstants.DEACTIVATING,
            CommonStatesConstants.INACTIVE));

    protected enum State {
        HEALTHY,
        UNHEALTHY,
        BAD,
        EXTRA
    }

    public ServiceDeploymentPlanner(Service service,
            DeploymentManagerContext context, Stack stack) {
        this.service = service;
        this.context = context;
        this.stack = stack;
        setHealthCheckAction(service, context);
        List<DeploymentUnit> units = context.objectManager.find(DeploymentUnit.class, DEPLOYMENT_UNIT.SERVICE_ID,
                service.getId(), DEPLOYMENT_UNIT.REMOVED, null, DEPLOYMENT_UNIT.STATE, new Condition(ConditionType.NE,
                        CommonStatesConstants.REMOVING));
        populateDeploymentUnits(units, context);
    }

    public String getStatus() {
        return String.format("Created: %d, Unhealthy: %d, Bad: %d",
                healthyUnits.size(),
                unhealthyUnits.size(),
                badUnits.size());
    }

    protected void populateDeploymentUnits(List<DeploymentUnit> units, DeploymentManagerContext context) {
        List<DeploymentUnit> healthyUnhealthy = new ArrayList<>();
        List<DeploymentUnit> healthy = new ArrayList<>();
        List<DeploymentUnit> unhealthy = new ArrayList<>();
        if (units != null) {
            for (DeploymentUnit unit : units) {
                List<String> errorStates = Arrays.asList(InstanceConstants.STATE_ERROR,
                        InstanceConstants.STATE_ERRORING);
                if (errorStates.contains(unit.getState())) {
                    addUnit(unit, State.BAD);
                } else {
                    healthyUnhealthy.add(unit);
                }
            }
            healthActionHandler.populateHealthyUnhealthyUnits(healthy, unhealthy,
                    healthyUnhealthy, context);
            for (DeploymentUnit healthyUnit : healthy) {
                addUnit(healthyUnit, State.HEALTHY);
            }

            for (DeploymentUnit unhealthyUnit : unhealthy) {
                addUnit(unhealthyUnit, State.UNHEALTHY);
            }
        }
    }

    protected void setHealthCheckAction(Service service, DeploymentManagerContext context) {
        Object healthCheckObj = ServiceDiscoveryUtil.getLaunchConfigObject(service,
                ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME, InstanceConstants.FIELD_HEALTH_CHECK);
        if (healthCheckObj != null) {
            InstanceHealthCheck healthCheck = context.jsonMapper.convertValue(healthCheckObj,
                    InstanceHealthCheck.class);
            if (healthCheck.getStrategy() == Strategy.none) {
                healthActionHandler = new NoopHealthCheckActionHandler();
            } else if (healthCheck.getStrategy() == Strategy.recreateOnQuorum) {
                if (healthCheck
                        .getRecreateOnQuorumStrategyConfig() == null) {
                    healthCheck.setRecreateOnQuorumStrategyConfig(new RecreateOnQuorumStrategyConfig(1));
                }
                healthActionHandler = new RecreateOnQuorumHealthCheckActionHandler(healthCheck
                        .getRecreateOnQuorumStrategyConfig().getQuorum());
            }
        }
    }

    public boolean isHealthcheckInitiailizing() {
        for (DeploymentUnit unit : this.allUnits.values()) {
            if (context.duMgr.isInit(unit)) {
                return true;
            }
        }
        return false;
    }

    private DeploymentUnitInstanceIdGenerator getIdGenerator() {
        List<Integer> usedIndexes = new ArrayList<>();
        for (DeploymentUnit du : getAllUnitsList()) {
            if (du.getServiceIndex() != null) {
                usedIndexes.add(Integer.valueOf(du.getServiceIndex()));
            }
        }
        return new DeploymentUnitInstanceIdGeneratorImpl(usedIndexes);
    }


    public List<DeploymentUnit> deploy() {
        /*
         * Cleanup first
         */
        cleanupUnits();

        /*
         * Schedule and wait for reconcile
         */
        List<DeploymentUnit> units = this.getUnits(getIdGenerator());
        sortByCreated(units);

        for (DeploymentUnit unit : units) {
            checkState();
            if (unit.getState().equalsIgnoreCase(CommonStatesConstants.INACTIVE)) {
                context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.ACTIVATE, unit, null);
            } else if (unit.getState().equalsIgnoreCase(CommonStatesConstants.REQUESTED)) {
                context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.CREATE,
                        unit, ProcessUtils.chainInData(new HashMap<String, Object>(),
                                ServiceConstants.PROCESS_DU_CREATE, ServiceConstants.PROCESS_DU_ACTIVATE));
            }
        }

        for (DeploymentUnit unit : units) {
            checkState();
            context.resourceMonitor.waitFor(unit,
                    new ResourcePredicate<DeploymentUnit>() {
                        @Override
                        public boolean evaluate(DeploymentUnit obj) {
                            if ((ERROR_STATES.contains(obj.getState()))
                                    || obj.getRemoved() != null) {
                                String error = TransitioningUtils.getTransitioningError(obj);
                                String message = "Bad deployment unit [" + key(obj) + "] in state [" + obj.getState()
                                        + "]";
                                if (StringUtils.isNotBlank(error)) {
                                    message = message + ": " + error;
                                }
                                throw new DeploymentUnitAllocateException(message, null,
                                        obj);
                            }
                            return CommonStatesConstants.ACTIVE.equals(obj.getState());
                        }

                        @Override
                        public String getMessage() {
                            return "active state";
                        }
                    });
        }
        
        return units;
    }

    protected void checkState() {
        List<String> pausedStates = Arrays.asList(ServiceConstants.STATE_PAUSED, ServiceConstants.STATE_PAUSING);
        if (pausedStates.contains(context.objectManager.reload(service).getState())) {
            throw new ProcessExecutionExitException(ExitReason.STATE_CHANGED);
        }
    }

    public void cleanupUnits() {
        cleanupBadUnits();
        processUnhealthyUnits();
    }

    protected void sortByCreated(List<DeploymentUnit> units) {
        // sort based on created date
        Collections.sort(units, new Comparator<DeploymentUnit>() {
            @Override
            public int compare(DeploymentUnit d1, DeploymentUnit d2) {
                boolean less = false;
                if (d1.getCreated().equals(d2.getCreated())) {
                    return Long.compare(Long.valueOf(d1.getServiceIndex()), Long.valueOf(d2.getServiceIndex()));
                }
                less = d1.getCreated().before(d2.getCreated());

                return less ? -1 : 1;
            }
        });
    }

    protected abstract List<DeploymentUnit> getUnits(DeploymentUnitInstanceIdGenerator svcInstanceIdGenerator);

    public boolean needToReconcileDeployment() {
        return unhealthyUnits.size() > 0 || badUnits.size() > 0
                || needToReconcileScale()
                || ifHealthyUnitsNeedReconcile();
    }

    private boolean ifHealthyUnitsNeedReconcile() {
        for (DeploymentUnit unit : this.healthyUnits.values()) {
            if (!unit.getState().equalsIgnoreCase(CommonStatesConstants.ACTIVE)) {
                return true;
            }
        }
        return false;
    }

    protected abstract boolean needToReconcileScale();

    protected void cleanupBadUnits() {
        List<DeploymentUnit> badUnits = new ArrayList<>();
        for (DeploymentUnit du : this.badUnits.values()) {
            badUnits.add(du);
        }
        List<DeploymentUnit> watchList = new ArrayList<>();
        for (DeploymentUnit badUnit : badUnits) {
            watchList.add(badUnit);
            cleanupUnit(badUnit, State.BAD, ServiceConstants.AUDIT_LOG_REMOVE_BAD, ActivityLog.ERROR);
        }
        for (DeploymentUnit toWatch : watchList) {
            waitForCleanup(toWatch);
        }
    }

    protected void processUnhealthyUnits() {
        List<DeploymentUnit> unheathyUnits = new ArrayList<>();
        for (DeploymentUnit du : this.unhealthyUnits.values()) {
            unheathyUnits.add(du);
        }
        for (DeploymentUnit unhealthyUnit : unheathyUnits) {
            context.objectProcessManager.scheduleProcessInstanceAsync(ServiceConstants.PROCESS_DU_UPDATE_UNHEALTHY,
                    unhealthyUnit, null);
        }
    }
    
    protected void addUnit(DeploymentUnit unit, State state) {
        if (state == State.UNHEALTHY) {
            unhealthyUnits.put(unit.getUuid(), unit);
        } else if (state == State.BAD) {
            badUnits.put(unit.getUuid(), unit);
        } else if (state == State.HEALTHY) {
            healthyUnits.put(unit.getUuid(), unit);
        }
        allUnits.put(unit.getUuid(), unit);
    }

    protected void removeFromList(DeploymentUnit unit, State state) {
        unhealthyUnits.remove(unit.getUuid());
        healthyUnits.remove(unit.getUuid());
        badUnits.remove(unit.getUuid());

        if (state == State.EXTRA) {
            allUnits.remove(unit.getUuid());
        }
    }

    public Map<String, DeploymentUnit> getAllUnits() {
        return allUnits;
    }

    protected List<DeploymentUnit> getAllUnitsList() {
        List<DeploymentUnit> units = new ArrayList<>();
        for (DeploymentUnit unit : allUnits.values()) {
            units.add(unit);
        }
        return units;
    }

    public void deactivateUnits() {
        List<String> validStates = Arrays.asList(CommonStatesConstants.ACTIVATING, CommonStatesConstants.ACTIVE,
                CommonStatesConstants.UPDATING_ACTIVE);
        for (DeploymentUnit unit : allUnits.values()) {
            if (validStates.contains(unit.getState())) {
                context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.DEACTIVATE, unit, null);
            }
        }
    }

    public void removeUnits() {
        List<DeploymentUnit> toRemove = new ArrayList<>();
        toRemove.addAll(allUnits.values());
        for (DeploymentUnit unit : toRemove) {
            removeUnit(unit, State.EXTRA, ServiceConstants.AUDIT_LOG_REMOVE_EXTRA, ActivityLog.INFO);
        }
    }

    protected void removeUnit(DeploymentUnit unit, State state, String reason, String level) {
        Map<String, Object> data = new HashMap<>();
        data.put(ServiceConstants.FIELD_DEPLOYMENT_UNIT_REMOVE_REASON, reason);
        data.put(ServiceConstants.FIELD_DEPLOYMENT_UNIT_REMOVE_LOG_LEVEL, level);
        List<String> ignoreStates = Arrays.asList(CommonStatesConstants.REMOVED, CommonStatesConstants.REMOVING);
        if (!ignoreStates.contains(unit.getState())) {
            try {
                context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, unit, data);
            } catch (ProcessCancelException e) {
                context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.DEACTIVATE,
                        unit, ProcessUtils.chainInData(data,
                                ServiceConstants.PROCESS_DU_DEACTIVATE, ServiceConstants.PROCESS_DU_REMOVE));
            }
        }

        removeFromList(unit, state);
    }

    protected void cleanupUnit(DeploymentUnit unit, State state, String reason, String level) {
        Map<String, Object> data = new HashMap<>();
        data.put(ServiceConstants.FIELD_DEPLOYMENT_UNIT_REMOVE_REASON, reason);
        data.put(ServiceConstants.FIELD_DEPLOYMENT_UNIT_REMOVE_LOG_LEVEL, level);
        data.put(ServiceConstants.FIELD_DEPLOYMENT_UNIT_CLEANUP, true);
        List<String> ignoreStates = Arrays.asList(CommonStatesConstants.INACTIVE, CommonStatesConstants.DEACTIVATING);
        if (!ignoreStates.contains(unit.getState())) {
            context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.DEACTIVATE, unit, data);

        }

        removeFromList(unit, state);
    }

    protected void waitForRemoval(DeploymentUnit unit) {
        context.resourceMonitor.waitFor(unit,
                new ResourcePredicate<DeploymentUnit>() {
                    @Override
                    public boolean evaluate(DeploymentUnit obj) {
                        List<String> removedStates = Arrays.asList(CommonStatesConstants.REMOVING,
                                CommonStatesConstants.REMOVED, CommonStatesConstants.PURGED,
                                CommonStatesConstants.PURGING);
                        return removedStates.contains(obj.getState());
                    }

                    @Override
                    public String getMessage() {
                        return "removing state";
                    }
                });
    }

    protected void waitForCleanup(DeploymentUnit unit) {
        context.resourceMonitor.waitFor(unit,
                new ResourcePredicate<DeploymentUnit>() {
                    @Override
                    public boolean evaluate(DeploymentUnit obj) {
                        return obj.getState().equalsIgnoreCase(CommonStatesConstants.INACTIVE);
                    }

                    @Override
                    public String getMessage() {
                        return "inactive state";
                    }
                });
    }

    protected String key(DeploymentUnit unit) {
        Object resourceId = context.idFormatter.formatId(unit.getKind(), unit.getId());
        return String.format("%s:%s", unit.getKind(), resourceId);
    }
}
