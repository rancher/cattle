package io.cattle.platform.docker.api.container;

import io.cattle.platform.api.action.ActionHandler;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

public class InspectContainerActionHandler implements ActionHandler {
    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
//
        //if (obj instanceof )
        return null;
    }

    @Override
    public String getName() {
        return "instance.inspect";
    }
}
