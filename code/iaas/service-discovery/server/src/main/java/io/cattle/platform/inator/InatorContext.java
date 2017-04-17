package io.cattle.platform.inator;

import java.util.Collections;
import java.util.Map;

public class InatorContext {

    Map<UnitRef, Unit> units;
    Inator inator;

    public Map<UnitRef, Unit> getUnits() {
        return units;
    }

    public Inator getInator() {
        return inator;
    }

    public InatorContext(Map<UnitRef, Unit> units, Inator inator) {
        super();
        this.units = Collections.unmodifiableMap(units);
        this.inator = inator;
    }

}
