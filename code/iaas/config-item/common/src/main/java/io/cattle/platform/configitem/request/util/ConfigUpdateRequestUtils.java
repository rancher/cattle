package io.cattle.platform.configitem.request.util;

import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfigUpdateRequestUtils {

    public static final String WAIT_FOR = "waitFor";

    public static ConfigUpdateRequest getRequest(JsonMapper jsonMapper, ProcessState state, Object id) {
        DataAccessor data = DataAccessor.fromMap(state.getData()).withScope(ConfigUpdateRequest.class).withKey(
                id instanceof String ? id.toString() : id.getClass().getName());

        return data.as(jsonMapper, ConfigUpdateRequest.class);
    }

    public static void setRequest(ConfigUpdateRequest request, ProcessState state, Object id) {
        DataAccessor data = DataAccessor.fromMap(state.getData()).withScope(ConfigUpdateRequest.class).withKey(
                id instanceof String ? id.toString() : id.getClass().getName());

        data.set(request);
    }

    public static List<ConfigUpdateRequest> getRequests(JsonMapper jsonMapper, ProcessState state) {
        List<ConfigUpdateRequest> result = new ArrayList<ConfigUpdateRequest>();
        Map<String, Object> requests = CollectionUtils.toMap(state.getData().get(ConfigUpdateRequest.class.getName()));

        for (Object obj : requests.values()) {
            result.add(jsonMapper.convertValue(obj, ConfigUpdateRequest.class));
        }

        return result;
    }

    public static void setWaitFor(ConfigUpdateRequest request) {
        request.getAttributes().put(WAIT_FOR, true);
    }

    public static boolean shouldWaitFor(ConfigUpdateRequest request) {
        return Boolean.TRUE.equals(request.getAttributes().get(WAIT_FOR));
    }
}
