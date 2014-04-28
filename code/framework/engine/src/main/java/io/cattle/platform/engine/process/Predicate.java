package io.cattle.platform.engine.process;

public interface Predicate {

    boolean evaluate(ProcessState state, ProcessInstance instance, ProcessDefinition definition);

}
