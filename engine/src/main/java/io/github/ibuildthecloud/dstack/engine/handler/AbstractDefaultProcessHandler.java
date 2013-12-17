package io.github.ibuildthecloud.dstack.engine.handler;

import io.github.ibuildthecloud.dstack.util.type.Priority;
import io.github.ibuildthecloud.dstack.util.type.ScopeUtils;

public abstract class AbstractDefaultProcessHandler extends AbstractProcessHandler implements Priority {

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

    @Override
    public String getDefaultScope() {
        return "process." + ScopeUtils.getScopeFromName(this) + ".handlers";
    }

}
