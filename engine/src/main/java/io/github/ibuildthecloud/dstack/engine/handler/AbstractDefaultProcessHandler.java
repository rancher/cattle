package io.github.ibuildthecloud.dstack.engine.handler;

import io.github.ibuildthecloud.dstack.util.type.Priority;
import io.github.ibuildthecloud.dstack.util.type.Scope;

public abstract class AbstractDefaultProcessHandler<T> extends AbstractProcessHandler implements Priority, Scope {

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

    @Override
    public String getDefaultScope() {
        return getDefaultScope(getName());
    }

    private static String getDefaultScope(String name) {
        return name.replaceAll("([a-z])([A-Z])", "$1.$2").toLowerCase();
    }

}
