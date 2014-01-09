package io.github.ibuildthecloud.dstack.process.base;

import io.github.ibuildthecloud.dstack.process.common.handler.AbstractObjectProcessHandler;
import io.github.ibuildthecloud.dstack.util.type.Priority;
import io.github.ibuildthecloud.dstack.util.type.ScopeUtils;

public abstract class AbstractDefaultProcessHandler extends AbstractObjectProcessHandler implements Priority {

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

    @Override
    public String[] getProcessNames() {
        return new String[] { ScopeUtils.getScopeFromName(this) };
    }

}
