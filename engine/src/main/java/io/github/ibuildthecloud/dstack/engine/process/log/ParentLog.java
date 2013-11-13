package io.github.ibuildthecloud.dstack.engine.process.log;

public interface ParentLog {

    public ProcessLog newChildLog();

}
