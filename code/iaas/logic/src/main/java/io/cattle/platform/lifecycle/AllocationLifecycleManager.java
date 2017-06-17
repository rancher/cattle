package io.cattle.platform.lifecycle;

import io.cattle.platform.core.model.Instance;

public interface AllocationLifecycleManager {

    void preStart(Instance instance);

    void postStop(Instance instance);

    void preRemove(Instance instance);

}
