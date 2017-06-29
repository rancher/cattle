package io.cattle.platform.lifecycle;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.lifecycle.util.LifecycleException;

public interface NetworkLifecycleManager {

    void create(Instance instance, Stack stack) throws LifecycleException;

    void preRemove(Instance instance);

    void assignNetworkResources(Instance instance) throws LifecycleException;

}
