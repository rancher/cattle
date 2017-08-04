package io.cattle.platform.lifecycle;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.lifecycle.util.LifecycleException;

public interface AllocationLifecycleManager {

    void preStart(Instance instance) throws LifecycleException;

    void postStop(Instance instance);

    void postRemove(Instance instance);

}
