package io.cattle.platform.lifecycle;

import io.cattle.platform.core.model.Instance;

public interface AgentLifecycleManager {

    void create(Instance instance);

    void preRemove(Instance instance);

}
