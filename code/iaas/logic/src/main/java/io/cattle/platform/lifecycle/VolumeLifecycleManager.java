package io.cattle.platform.lifecycle;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.lifecycle.util.LifecycleException;

public interface VolumeLifecycleManager {

    void create(Instance instance) throws LifecycleException;

    void preStart(Instance instance) throws LifecycleException;

    void preRemove(Instance instance);

}
