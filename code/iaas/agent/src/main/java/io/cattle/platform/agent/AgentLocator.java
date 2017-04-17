package io.cattle.platform.agent;

public interface AgentLocator {

    RemoteAgent lookupAgent(Object resource);

}
