package io.github.ibuildthecloud.dstack.engine.process.log;

import io.github.ibuildthecloud.dstack.util.type.Named;

public interface ParentLog extends Named {

    public ProcessLog newChildLog();

}
