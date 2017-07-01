package io.cattle.platform.lifecycle;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceIndex;

public interface ServiceLifecycleManager {

    void preRemove(Instance instance);

    void postRemove(Instance instance);

    boolean isSelectorContainerMatch(String selector, Instance instance);

    void releaseIpFromServiceIndex(Service service, ServiceIndex serviceIndex);

    void remove(Service service);

    void create(Service service);

}
