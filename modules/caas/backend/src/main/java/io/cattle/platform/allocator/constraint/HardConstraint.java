package io.cattle.platform.allocator.constraint;

public abstract class HardConstraint implements Constraint {

    public final boolean isHardConstraint() {
        return true;
    }
}
