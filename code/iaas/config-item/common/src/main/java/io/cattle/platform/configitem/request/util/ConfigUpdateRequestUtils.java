package io.cattle.platform.configitem.request.util;

import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;

public class ConfigUpdateRequestUtils {

    public static ConfigUpdateRequest getRequest(JsonMapper jsonMapper, ProcessState state, Object context) {
        DataAccessor data = DataAccessor
                .fromMap(state.getData())
                .withScope(ConfigUpdateRequest.class)
                .withKey(context instanceof String ? context.toString() : context.getClass().getName());

        return data.as(jsonMapper, ConfigUpdateRequest.class);
    }

    public static void setRequest(ConfigUpdateRequest request, ProcessState state, Object context) {
        DataAccessor data = DataAccessor
                .fromMap(state.getData())
                .withScope(ConfigUpdateRequest.class)
                .withKey(context instanceof String ? context.toString() : context.getClass().getName());

        data.set(request);
    }
}
