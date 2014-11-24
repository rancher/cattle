package io.cattle.platform.configitem.server.agentinclude;

import java.util.List;
import java.util.Map;

public interface AgentIncludeMap {

    List<String> getNamedMaps();

    Map<String,String> getMap(String name);

    String getSourceRevision(String name);

}
