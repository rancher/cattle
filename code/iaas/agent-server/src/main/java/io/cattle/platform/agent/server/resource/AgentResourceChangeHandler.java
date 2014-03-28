package io.cattle.platform.agent.server.resource;

import io.cattle.platform.agent.server.resource.impl.MissingDependencyException;

import java.util.Map;

public interface AgentResourceChangeHandler {

    String getType();

    Map<String,Object> load(String uuid);

    void newResource(long agentId, Map<String,Object> resource) throws MissingDependencyException;

    boolean areDifferent(Map<String,Object> agentResource, Map<String,Object> loadedResource);

    void changed(Map<String,Object> agentResource, Map<String,Object> loadedResource);

}
