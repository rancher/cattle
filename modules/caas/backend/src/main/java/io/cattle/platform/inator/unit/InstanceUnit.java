package io.cattle.platform.inator.unit;

import io.cattle.platform.core.addon.DependsOn;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.inator.Inator;
import io.cattle.platform.inator.Inator.DesiredState;
import io.cattle.platform.inator.InatorContext;
import io.cattle.platform.inator.Result;
import io.cattle.platform.inator.Unit;
import io.cattle.platform.inator.UnitRef;
import io.cattle.platform.inator.deploy.DeploymentUnitInator;
import io.cattle.platform.inator.factory.InatorServices;
import io.cattle.platform.inator.launchconfig.LaunchConfig;
import io.cattle.platform.inator.wrapper.BasicStateWrapper;
import io.cattle.platform.inator.wrapper.DeploymentUnitWrapper;
import io.cattle.platform.inator.wrapper.InstanceWrapper;
import io.cattle.platform.inator.wrapper.StackWrapper;
import io.github.ibuildthecloud.gdapi.util.DateUtils;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InstanceUnit implements Unit, BasicStateUnit {

    String name;
    LaunchConfig lc;
    InstanceWrapper instance;
    UnitRef ref;
    InatorServices svc;

    // Needed for create only
    StackWrapper stack;
    DeploymentUnitWrapper unit;

    public InstanceUnit(InstanceWrapper instance, LaunchConfig lc, StackWrapper stack, DeploymentUnitWrapper unit, InatorServices svc) {
        super();
        this.instance = instance;
        this.lc = lc;
        this.name = this.lc.getName();
        this.svc = svc;
        this.unit = unit;
        this.stack = stack;
        this.ref = new UnitRef("instance/" + lc.getRevision() + "/" + name);
    }

    public InstanceUnit(String name, LaunchConfig lc, StackWrapper stack, DeploymentUnitWrapper unit, InatorServices svc) {
        this.name = name;
        this.lc = lc;
        this.stack = stack;
        this.unit = unit;
        this.svc = svc;
        this.ref = new UnitRef("instance/" + lc.getRevision() + "/" + name);
    }

    @Override
    public Result define(InatorContext context, boolean desired) {
        if (!desired) {
            if (instance != null) {
                instance.setDesired(false);
            }
            return Result.good();
        }

        if (instance == null) {
            instance = lc.create(context, stack, unit);
        }

        if (instance.isTransitioning()) {
            return new Result(UnitState.WAITING, this, String.format("Waiting for create %s", getDisplayName()));
        }

        if (CommonStatesConstants.REQUESTED.equals(instance.getState())) {
            instance.create();
            return new Result(UnitState.WAITING, this, String.format("Creating %s", getDisplayName()));
        }

        applyDynamic(context);
        instance.setDesired(true);

        return Result.good();
    }

    protected Result applyDynamic(InatorContext context) {
        Inator inator = context.getInator();
        if (inator instanceof DeploymentUnitInator) {
            LaunchConfig currentLc = ((DeploymentUnitInator) inator).getRevision().getLaunchConfig(name);
            if (currentLc != null) {
                return currentLc.applyDynamic(instance, context);
            }
        }
        return Result.good();
    }

    @Override
    public Set<UnitRef> dependencies(InatorContext context) {
        Set<UnitRef> deps = new HashSet<>(lc.getDependencies().keySet());
        Set<UnitRef> desiredSet = context.getInator().getDesiredRefs();
        UnitRef selfRef = getRef();

        for (UnitRef otherRef : context.getUnits().keySet()) {
            if (!isSameLaunchConfig(otherRef)) {
                continue;
            }

            if (desiredSet.contains(selfRef)) {
                // We are replacing a different container
                if (!lc.isStartFirst()) {
                    deps.add(otherRef);
                }
            } else {
                // We are being replaced
                if (lc.isStartFirst()) {
                    deps.add(otherRef);
                }
            }
        }

        return deps;
    }

    @Override
    public Result preRemove(InatorContext context) {
        boolean shouldRemove = svc.deploymentConditions.removeBackoff.check(unit.getId(),
                lc.getName(),
                () -> svc.triggerDeploymentUnitReconcile(unit.getId()));
        if (!shouldRemove) {
            return new Result(UnitState.WAITING, this, "Remove backoff");
        }
        return Result.good();
    }

    @Override
    public Result remove(InatorContext context) {
        return remove(context, false);
    }

    protected Result remove(InatorContext context, boolean forceDelete) {
        if (instance == null) {
            return Result.good();
        }

        if (!instance.isServiceManaged()) {
            instance.setDesired(false);
            return BasicStateUnit.super.remove(context);
        }

        /*
         * This controls whether we really deleted the instance or just stop it
         * and mark it for deletion later.  Desired means delete now and not-desired
         * means stop and delete later.
         */
        boolean desired = true;
        if (context.getInator().getDesiredState() != DesiredState.REMOVED) {
            desired = false;
        }

        if (forceDelete) {
            desired = true;
        }

        instance.setDesired(forceDelete || desired);
        return desired ? BasicStateUnit.super.remove(context) : deactivate(context);
    }

    @Override
    public UnitRef getRef() {
        return ref;
    }

    protected boolean isSameLaunchConfig(UnitRef unitRef) {
        String[] parts = unitRef.toString().split("/");
        return parts.length == 3 &&
                "instance".equals(parts[0]) &&
                name.equals(parts[2]) &&
                !unitRef.toString().equals(getRef().toString());
    }

    @Override
    public BasicStateWrapper getWrapper() {
        return instance;
    }

    public Date getStartTime() {
        return instance.getStartTime();
    }

    @Override
    public Result removeBad(InatorContext context, BasicStateUnit.RemoveReason reason) {
        switch (reason) {
        case ERROR:
            if (instance.isServiceManaged()) {
                return removeBad(context, String.format("Error on %s: %s", getDisplayName(),
                        instance.getErrorMessage()), reason);
            }
            return Result.good();
        case UNHEALTHY:
            return removeBad(context, String.format("Unhealthy %s", getDisplayName(),
                    instance.getErrorMessage()), reason);
        default:
            return removeBad(context, "Removing bad instance %s", reason);
        }
    }


    protected Result removeBad(InatorContext context, String message, RemoveReason reason) {
        Result result = null;
        if (reason == RemoveReason.UNHEALTHY && lc.isHealthcheckActionNone()) {
            return Result.good();
        }

        result = remove(context, true);
        if (result.isGood()) {
            return result;
        }
        result = new Result(UnitState.WAITING, this, String.format(message, getDisplayName()));

        if (reason == RemoveReason.ERROR) {
            result.setLogLevel("error");
        }
        return result;
    }

    public Long getInstanceId() {
        return instance.getId();
    }

    @Override
    public Result activate(InatorContext context) {
        // Make sure our dependencies haven't been rebuilt
        if (!lc.validateDeps(context, instance)) {
            // TODO: This really shouldn't force delete
            return removeBad(context, "Missing or mismatched dependency", RemoveReason.OTHER);
        }

        // Do actual start
        Result result = BasicStateUnit.super.activate(context);
        if (!result.isGood()) {
            return result;
        }

        // Restart self if dependency is started after self
        Date selfStart = instance.getStartTime();
        Map<UnitRef, Unit> units = context.getUnits();
        for (UnitRef dep : dependencies(context)) {
            Unit unit = units.get(dep);
            if (!(unit instanceof InstanceUnit)) {
                continue;
            }

            Date startTime = ((InstanceUnit) unit).getStartTime();
            if (selfStart != null && startTime != null && selfStart.before(startTime)) {
                instance.deactivate();
                return new Result(UnitState.WAITING, this,
                        String.format("Stopping %s because dependency restarted", getDisplayName()));
            }
        }

        return result;
    }

    @Override
    public Result preActivate(InatorContext context) {
        Result result = Result.good();

        Runnable callback = () -> svc.triggerDeploymentUnitReconcile(unit.getId());
        for (DependsOn dep : lc.getDependsOn()) {
            if (dep.getCondition() == DependsOn.DependsOnCondition.healthylocal && unit.getHostId() == null) {
                // Can't evaluate now, have to do it in instance.start
                continue;
            }

            if (!svc.deploymentConditions.serviceDependency.satified(stack.getAccountId(), stack.getId(),
                    unit.getHostId(), dep, callback)) {
                result.aggregate(new Result(UnitState.WAITING, this,
                        String.format("Waiting on dependency: %s", dep.getDisplayName())));
            }
        }

        if (result.isGood()) {
            Long runAfter = instance.startBackoff();
            if (runAfter != null) {
                return new Result(UnitState.WAITING, this, String.format("Start backoff, retry %s",
                        DateUtils.toString(new Date(runAfter))));
            }
        }

        return result;
    }

    @Override
    public String getDisplayName() {
        return instance == null ? "(missing)" : instance.getDisplayName();
    }
}