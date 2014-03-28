package io.cattle.platform.process.base;

import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.util.type.Priority;

public abstract class AbstractDefaultProcessHandler extends AbstractObjectProcessHandler implements Priority {

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

    @Override
    public String[] getProcessNames() {
        return new String[] { ProcessUtils.getDefaultProcessName(this) };
    }

}
