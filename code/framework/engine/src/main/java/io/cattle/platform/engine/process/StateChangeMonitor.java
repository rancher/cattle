package io.cattle.platform.engine.process;

import io.cattle.platform.engine.manager.impl.ProcessRecord;

public interface StateChangeMonitor {

    void onChange(boolean schedule, String previousState, String newState, ProcessRecord record, ProcessState state, ProcessServiceContext context);

}
