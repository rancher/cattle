package io.github.ibuildthecloud.dstack.engine.process.log;

import java.util.ArrayList;
import java.util.List;

public class AbstractParentLog implements ParentLog {

    List<ProcessLog> children = new ArrayList<ProcessLog>();

    @Override
    public ProcessLog newChildLog() {
        ProcessLog log = new ProcessLog();
        children.add(log);
        return log;
    }

}
