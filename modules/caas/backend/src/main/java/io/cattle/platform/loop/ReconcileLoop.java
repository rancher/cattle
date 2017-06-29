package io.cattle.platform.loop;

import io.cattle.platform.activity.ActivityService;
import io.cattle.platform.engine.model.Loop;
import io.cattle.platform.inator.Deployinator;
import io.cattle.platform.inator.Unit;
import io.cattle.platform.inator.Unit.UnitState;
import io.cattle.platform.inator.unit.InstanceUnit;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public class ReconcileLoop implements Loop {

    Deployinator deployinator;
    ActivityService activityService;
    Class<?> clz;
    Long id, serviceId, deploymentUnitId, accountId;
    String type;
    ObjectProcessManager processManager;
    ObjectManager objectManager;

    public ReconcileLoop(ObjectManager objectManager, ObjectProcessManager processManager, Deployinator deployinator,
            ActivityService activityService, Class<?> clz, Long id, Long serviceId, Long deploymentUnitId,
            Long accountId, String type) {
        super();
        this.objectManager = objectManager;
        this.processManager = processManager;
        this.deployinator = deployinator;
        this.clz = clz;
        this.id = id;
        this.activityService = activityService;
        this.serviceId = serviceId;
        this.deploymentUnitId = deploymentUnitId;
        this.type = type;
        this.accountId = accountId;
    }

    @Override
    public Result run(Object input) {
        List<io.cattle.platform.inator.Result> result = new ArrayList<>();
        activityService.run(accountId, serviceId, deploymentUnitId, type, "Reconciling", () -> {
            result.add(reconcile());
        });
        io.cattle.platform.inator.Result r = result.get(0);
        if (r.isGood()) {
            return Result.DONE;
        } else if (r.getState() == UnitState.WAITING) {
            return Result.WAITING;
        }
        return null;
    }

    protected io.cattle.platform.inator.Result reconcile() {
        io.cattle.platform.inator.Result result = deployinator.reconcile(clz, id);
        if (result.getState() == UnitState.ERROR) {
            processManager.error(objectManager.loadResource(clz, id), null);
        }
        logResult(result, new HashSet<>());
        if (result.getState() == UnitState.WAITING) {
            activityService.waiting();
        }
        if ("error".equals(result.getLogLevel())) {
            activityService.error("Result: %s %s", result.getState(), result.getUnit() == null ? "" : result.getUnit().getDisplayName());
        } else if (!result.isGood() || result.getUnit() != null) {
            activityService.info("Result: %s %s", result.getState(), result.getUnit() == null ? "" : result.getUnit().getDisplayName());
        }
        return result;
    }

    protected void logResult(io.cattle.platform.inator.Result result, Set<io.cattle.platform.inator.Result> seen) {
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

        for (io.cattle.platform.inator.Result subResult : result.getResults()) {
            logResult(subResult, seen);
        }
    }


    @Override
    public String toString() {
        return "loop [" + clz.getSimpleName().toLowerCase() + ":" + id + "]";
    }

}
