package io.github.ibuildthecloud.dstack.api.action;

import io.github.ibuildthecloud.dstack.util.type.Named;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

public interface ActionHandler extends Named {

    Object perform(String name, Object obj, ApiRequest request);

}
