package io.cattle.platform.loop;

import io.cattle.platform.engine.model.Loop;
import io.cattle.platform.inator.Deployinator;
import io.cattle.platform.inator.Unit.UnitState;

import java.util.Date;

public class ReconcileLoop implements Loop {

    Deployinator deployinator;
    Class<?> clz;
    Long id;

    public ReconcileLoop(Deployinator deployinator, Class<?> clz, Long id) {
        super();
        this.deployinator = deployinator;
        this.clz = clz;
        this.id = id;
    }

    @Override
    public Result run(Object input) {
        io.cattle.platform.inator.Result r = deployinator.reconcile(clz, id);
        System.err.println(new Date() + " [" + toString() + "] " + r.getState());
        if (r.isGood()) {
            return Result.DONE;
        } else if (r.getState() == UnitState.WAITING) {
            return Result.WAITING;
        }
        return null;
    }

    @Override
    public String toString() {
        return "loop [" + clz.getSimpleName().toLowerCase() + ":" + id + "]";
    }

}
