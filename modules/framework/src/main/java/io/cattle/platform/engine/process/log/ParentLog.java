package io.cattle.platform.engine.process.log;

import io.cattle.platform.util.type.Named;

public interface ParentLog extends Named {

    ProcessLog newChildLog();

}
