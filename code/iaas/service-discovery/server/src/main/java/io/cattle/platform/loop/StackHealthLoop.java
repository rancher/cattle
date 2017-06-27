package io.cattle.platform.loop;

import io.cattle.platform.engine.model.Loop;
import io.cattle.platform.lifecycle.ServiceLifecycleManager;

public class StackHealthLoop implements Loop {

    ServiceLifecycleManager sdService;
    Long stackId;

    public StackHealthLoop(ServiceLifecycleManager sdService, Long id) {
        super();
        this.sdService = sdService;
        this.stackId = id;
    }

    @Override
    public Result run(Object input) {
        sdService.updateHealthState(stackId);
        return Result.DONE;
    }

}
