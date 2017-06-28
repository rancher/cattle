package io.cattle.platform.endpoint.loop;

import io.cattle.platform.engine.model.Loop;
import io.cattle.platform.environment.EnvironmentResourceManager;
import io.cattle.platform.object.ObjectManager;

public class EndpointUpdateLoop implements Loop {

    long accountId;
    EnvironmentResourceManager envResourceManager;
    ObjectManager objectManager;

    public EndpointUpdateLoop(long accountId, EnvironmentResourceManager envResourceManager, ObjectManager objectManager) {
        this.accountId = accountId;
        this.envResourceManager = envResourceManager;
        this.objectManager = objectManager;
    }


}
