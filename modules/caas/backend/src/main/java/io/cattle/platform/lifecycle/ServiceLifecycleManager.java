package io.cattle.platform.lifecycle;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;

public interface ServiceLifecycleManager {

    void postRemove(Instance instance);

    void remove(Service service);

    void create(Service service);

}
