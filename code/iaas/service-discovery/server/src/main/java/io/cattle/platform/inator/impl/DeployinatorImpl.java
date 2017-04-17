package io.cattle.platform.inator.impl;

import io.cattle.platform.activity.ActivityService;
import io.cattle.platform.configitem.events.ConfigUpdate;
import io.cattle.platform.configitem.model.Client;
import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.inator.Deployinator;
import io.cattle.platform.inator.Inator;
import io.cattle.platform.inator.InatorContext;
import io.cattle.platform.inator.Result;
import io.cattle.platform.inator.Unit;
import io.cattle.platform.inator.Unit.UnitState;
import io.cattle.platform.inator.UnitRef;
import io.cattle.platform.inator.factory.InatorFactoryinator;
import io.cattle.platform.inator.lock.ReconcileLock;
import io.cattle.platform.inator.unit.MetaUnit;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class DeployinatorImpl implements Deployinator {

    private static final String RECONCILE = "reconcile";
    private static final String DU_RECONCILE = "deployment-unit-update";

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

    @Override
    public Result reconcile(Class<?> clz, Long id) {
        String type = objectManager.getType(clz);
        return lockManager.lock(new ReconcileLock(type, id), () -> reconcile(type, id));
    }

    @Override
    public void serviceUpdate(ConfigUpdate update) {
        final Client client = new Client(Service.class, new Long(update.getResourceId()));
        itemManager.runUpdateForEvent(RECONCILE, update, client, new Runnable() {
            @Override
            public void run() {
                final Service service = objectManager.loadResource(Service.class, client.getResourceId());
                activitySvc.run(service, "service.trigger", "Re-evaluating state", new Runnable() {
                    @Override
                    public void run() {
                        reconcile(Service.class, client.getResourceId());
                    }
                });
            }
        });
    }

    @Override
    public void reconcileServices(Collection<? extends Service> services) {
        for (Service service: services) {
            ConfigUpdateRequest request = ConfigUpdateRequest.forResource(Service.class, service.getId());
            request.addItem(RECONCILE);
            request.withDeferredTrigger(true);
            itemManager.updateConfig(request);
        }
    }

    @Override
    public void scheduleReconcile(DeploymentUnit unit) {
        ConfigUpdateRequest request = ConfigUpdateRequest.forResource(DeploymentUnit.class, unit.getId());
        request.addItem(DU_RECONCILE);
        request.withDeferredTrigger(true);
        itemManager.updateConfig(request);
    }

    @Override
    public void deploymentUnitUpdate(ConfigUpdate update) {
        final Client client = new Client(DeploymentUnit.class, new Long(update.getResourceId()));
        itemManager.runUpdateForEvent(DU_RECONCILE, update, client, () -> {
            DeploymentUnit unit = objectManager.loadResource(DeploymentUnit.class, client.getResourceId());
            activitySvc.run(null, unit, "deploymentunit.trigger", "Re-evaluating deployment unit state",
                    new Runnable() {
                @Override
                public void run() {
                    reconcile(DeploymentUnit.class, client.getResourceId());
                }
            });
        });
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

        switch (inator.getDesiredState()) {
        case ACTIVE:
            return activate(context);
        case INACTIVE:
            return deactivate(context);
        case REMOVED:
            return remove(context);
        case NONE:
            return Result.good();
        default:
            break;
        }

        return Result.good();
    }

    private Result remove(InatorContext context) {
        for (Unit unit : context.getUnits().values()) {
            unit.remove();
        }

        return Result.good();
    }

    protected Result deactivate(InatorContext context) {
        return recurse(new HashMap<>(), new MetaUnit(), context);
    }

    private Result activate(InatorContext context) {
        for (Unit unit : context.getUnits().values()) {
            unit.define(context);
        }

        return recurse(new HashMap<>(), new MetaUnit(), context);
    }

    private Result recurse(Map<UnitRef, Result> seen, Unit unit, InatorContext context) {
        Result result = seen.get(unit.getRef());
        if (result != null) {
            return result;
        }
        result = Result.good();

        for (UnitRef ref : unit.dependencies(context)) {
            Result subResult = recurse(seen, context.getUnits().get(ref), context);
            result.aggregate(subResult);
        }

        if (result.getState() == UnitState.GOOD) {
            if (context.getInator().getDesiredUnits().contains(unit.getRef())) {
                Unit.UnitState state = unit.scheduleActions(context);
                result.aggregate(new Result(state, unit));
            } else {
                if (unit.remove()) {
                    result.aggregate(Result.good());
                } else {
                    result.aggregate(new Result(UnitState.WAITING, unit));
                }
            }
            Unit.UnitState state = unit.scheduleActions(context);
            result.aggregate(new Result(state, unit));
        }

        seen.put(unit.getRef(), result);
        return result;
    }

}
