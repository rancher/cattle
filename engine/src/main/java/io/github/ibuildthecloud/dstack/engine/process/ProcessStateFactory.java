package io.github.ibuildthecloud.dstack.engine.process;

import io.github.ibuildthecloud.dstack.util.type.Named;

public interface ProcessStateFactory extends Named {

    ProcessState constructProcessState(LaunchConfiguration config);

}
