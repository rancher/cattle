package io.cattle.platform.api.action;

import io.cattle.platform.util.type.Named;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

public interface ActionHandler extends Named {

    Object perform(String name, Object obj, ApiRequest request);

}
