package io.cattle.platform.lifecycle;

import io.cattle.platform.core.model.Instance;

public interface SecretsLifecycleManager {

    Object create(Instance instance);

    void persistCreate(Instance instance, Object obj);

}
