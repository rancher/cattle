package io.cattle.platform.lifecycle;

import io.cattle.platform.core.model.Instance;

public interface VirtualMachineLifecycleManager {

    void instanceCreate(Instance instance);

}
