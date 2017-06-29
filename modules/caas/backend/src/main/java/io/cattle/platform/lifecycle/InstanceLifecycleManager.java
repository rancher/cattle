package io.cattle.platform.lifecycle;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.lifecycle.util.LifecycleException;

public interface InstanceLifecycleManager {

    void create(Instance instance) throws LifecycleException;

    void preStart(Instance instance) throws LifecycleException;

    void postStart(Instance instance);

    void postStop(Instance instance, boolean stopOnly);

    void preRemove(Instance instance);

    void postRemove(Instance instance);

}
