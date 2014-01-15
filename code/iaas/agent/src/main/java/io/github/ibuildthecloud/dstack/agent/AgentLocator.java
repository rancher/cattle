package io.github.ibuildthecloud.dstack.agent;

public interface AgentLocator {

    RemoteAgent lookupAgent(Object resource);

}
