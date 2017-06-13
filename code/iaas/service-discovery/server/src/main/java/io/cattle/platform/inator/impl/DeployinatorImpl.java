package io.cattle.platform.inator.impl;

import io.cattle.platform.activity.ActivityService;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.inator.Deployinator;
import io.cattle.platform.inator.Inator;
import io.cattle.platform.inator.InatorContext;
import io.cattle.platform.inator.PausableUnit;
import io.cattle.platform.inator.Result;
import io.cattle.platform.inator.Unit;
import io.cattle.platform.inator.UnitRef;
import io.cattle.platform.inator.factory.InatorFactoryinator;
import io.cattle.platform.inator.lock.ReconcileLock;
import io.cattle.platform.inator.unit.AllUnits;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class DeployinatorImpl implements Deployinator {

    @Inject
    InatorFactoryinator factory;
    @Inject
    ObjectManager objectManager;
    @Inject
    LockManager lockManager;
    @Inject
    ConfigItemStatusManager itemManager;
    @Inject
    ActivityService activitySvc;
    @Inject
    ServiceDiscoveryService sdService;

    @Override
    public Result reconcile(Class<?> clz, Long id) {
        String type = objectManager.getType(clz);
        return lockManager.lock(new ReconcileLock(type, id), () -> reconcile(type, id));
    }


    protected Result reconcile(String type, Long id) {
        Inator inator = factory.buildInator(type, id);
        if (inator == null) {
            return Result.good();
        }

        Map<UnitRef, Unit> units = new HashMap<>();
        for (Unit unit : inator.collect()) {
            units.put(unit.getRef(), unit);
        }

        units = inator.fillIn(new InatorContext(units, inator));

        InatorContext context = new InatorContext(units, inator);

        Result result = null;
        switch (inator.getDesiredState()) {
        case ACTIVE:
            result = activate(context);
            break;
        case INACTIVE:
            result = deactivate(context);
            break;
        case REMOVED:
            result = remove(context);
            break;
        case PAUSE:
            result = pause(context);
            break;
        case ERROR:
            return Result.good();
        default:
            break;
        }

        return inator.postProcess(context, result);
    }

    private Result remove(InatorContext context) {
        return recurse(context, false, (unit) -> unit.remove(context));
    }

    protected Result pause(InatorContext context) {
        return recurse(context, false, (unit) -> {
            if (unit instanceof PausableUnit) {
                return ((PausableUnit) unit).pause();
            }
            return Result.good();
        });
    }

    protected Result deactivate(InatorContext context) {
        return recurse(context, false, (unit) -> unit.scheduleActions(context));
    }

    private Result activate(InatorContext context) {
        Result result = recurse(new HashMap<>(), new AllUnits(), context, false, (unit) -> {
            boolean desired = context.getInator().getDesiredRefs().contains(unit.getRef());
            return unit.define(context, desired);
        });
        if (!result.isGood()) {
            return result;
        }

        return recurse(context, true, (unit) -> {
            if (context.getInator().getDesiredRefs().contains(unit.getRef())) {
                return unit.scheduleActions(context);
            } else {
                return unit.remove(context);
            }
        });
    }

    /**
     * @param seen
     * @param unit
     * @param context
     * @param checkResult false means don't stop recursing on non-GOOD, also don't remove undesired units
     * @param fun
     * @return
     */
    private Result recurse(Map<UnitRef, Result> seen, Unit unit, InatorContext context, boolean checkResult, Function<Unit, Result> fun) {
        Result result = seen.get(unit.getRef());
        if (result != null) {
            return result;
        }
        result = Result.good();

        for (UnitRef ref : unit.dependencies(context)) {
            Unit depUnit = context.getUnits().get(ref);
            if (depUnit == null) {
                continue;
            }

            Result subResult = recurse(seen, depUnit, context, checkResult, fun);
            result.aggregate(subResult);
        }

        if (!checkResult || result.isGood()) {
            result.aggregate(fun.apply(unit));
        }

        seen.put(unit.getRef(), result);
        return result;
    }

    private Result recurse(InatorContext context, boolean checkResult, Function<Unit, Result> fun) {
        return recurse(context.getResults(), new AllUnits(), context, checkResult, fun);

    }
}
