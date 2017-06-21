package io.cattle.platform.lifecycle;

import io.cattle.platform.core.model.Instance;

public interface ServiceLifecycleManager {

    void preRemove(Instance instance);

    void postRemove(Instance instance);

}
