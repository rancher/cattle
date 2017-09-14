package io.cattle.platform.api.handler;

import com.netflix.config.DynamicStringListProperty;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.framework.event.FrameworkEvents;
import io.cattle.platform.util.exception.ExceptionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.model.Schema.Method;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.ApiRequestHandler;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EventNotificationHandler implements ApiRequestHandler {

    private static final DynamicStringListProperty EXCLUDE = ArchaiusUtil.getList("api.event.change.exclude.types");

    EventService eventService;
    Set<String> excludeTypes = new HashSet<>();

    public EventNotificationHandler(EventService eventService) {
        this.eventService = eventService;
        init();
    }

    @Override
    public void handle(ApiRequest request) throws IOException {
        if (Method.GET.isMethod(request.getMethod())) {
            return;
        }

        String type = request.getSchemaFactory().getBaseType(request.getType());

        if (excludeTypes.contains(type)) {
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("method", request.getMethod().toString());
        data.put("id", request.getId());
        data.put("type", type);
        data.put("action", request.getAction());
        data.put("responseCode", request.getResponseCode());
        data.put("accountId", ApiUtils.getPolicy().getAccountId());
        data.put("clusterId", ApiUtils.getPolicy().getClusterId());

        DeferredUtils.deferPublish(eventService,
                EventVO.newEvent(FrameworkEvents.API_CHANGE)
                        .withResourceId(request.getId())
                        .withResourceType(type)
                        .withData(data));
    }

    @Override
    public boolean handleException(ApiRequest request, Throwable t) throws IOException, ServletException {
        if (t instanceof ClientVisibleException) {
            return false;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("method", request.getMethod().toString());
        data.put("id", request.getId());
        data.put("type", request.getType());
        data.put("action", request.getAction());

        data.put("message", t.getMessage());
        data.put("stackTrace", ExceptionUtils.toString(t));

        DeferredUtils.deferPublish(eventService, EventVO
                .newEvent(FrameworkEvents.API_EXCEPTION)
                .withData(data));

        return false;
    }

    private void init() {
        load();
        EXCLUDE.addCallback(new Runnable() {
            @Override
            public void run() {
                load();
            }
        });
    }

    public void load() {
        excludeTypes = new HashSet<>();
        excludeTypes.addAll(EXCLUDE.get());
    }

}
