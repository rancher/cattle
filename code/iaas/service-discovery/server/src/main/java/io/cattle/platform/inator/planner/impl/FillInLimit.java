package io.cattle.platform.inator.planner.impl;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.inator.Unit;
import io.cattle.platform.inator.UnitRef;
import io.cattle.platform.inator.unit.WaitingUnit;

import java.util.Map;

import com.netflix.config.DynamicIntProperty;

public class FillInLimit {

    private static final DynamicIntProperty MAX = ArchaiusUtil.getInt("deployment.unit.batch.size");

    int count = 0;

    public boolean add(Map<UnitRef, Unit> units, Unit unit) {
        if (count >= MAX.get()) {
            WaitingUnit waiting = new WaitingUnit(String.format("Creating max batch size %d", MAX.get()));
            units.put(waiting.getRef(), waiting);
            return false;
        }
        count++;
        units.put(unit.getRef(), unit);
        return true;
    }

}
