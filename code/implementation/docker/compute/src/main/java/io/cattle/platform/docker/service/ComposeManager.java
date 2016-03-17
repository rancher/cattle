package io.cattle.platform.docker.service;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;

public interface ComposeManager {

    void setupServiceAndInstance(Instance instance);

    void cleanupResources(Service service);

}