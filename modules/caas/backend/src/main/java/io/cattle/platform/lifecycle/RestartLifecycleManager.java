package io.cattle.platform.lifecycle;

import io.cattle.platform.core.model.Instance;

public interface RestartLifecycleManager {

    void postStart(Instance instance);

    void postStop(Instance instance, boolean stopOnly);

}
