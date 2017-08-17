package io.cattle.platform.api.pubsub.manager;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.pubsub.model.Subscribe;
import io.cattle.platform.api.pubsub.subscribe.SubscriptionHandler;
import io.cattle.platform.api.pubsub.util.SubscriptionUtils;
import io.cattle.platform.api.pubsub.util.SubscriptionUtils.SubscriptionStyle;
import io.cattle.platform.api.resource.AbstractNoOpResourceManager;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.framework.event.FrameworkEvents;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ProxyUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class SubscribeManager extends AbstractNoOpResourceManager {

    public static final String EVENT_DISCONNECT = "disconnect";
    SubscriptionHandler[] handlers;

    public SubscribeManager(SubscriptionHandler... handlers) {
        super();
        this.handlers = handlers;
    }

    @Override
    public Object create(String type, ApiRequest request) {
        List<String> eventNames = getEventNames(request);
        Set<String> filteredEventNames = new TreeSet<>();

        Policy policy = ApiUtils.getPolicy();

        SubscriptionStyle style = SubscriptionUtils.getSubscriptionStyle(policy);
        for (String eventName : eventNames) {
            switch (style) {
            case QUALIFIED:
                if (eventName.contains(FrameworkEvents.EVENT_SEP)) {
                    eventName = StringUtils.substringBefore(eventName, FrameworkEvents.EVENT_SEP);
                }

                String key = SubscriptionUtils.getSubscriptionQualifier(policy);
                String value = SubscriptionUtils.getSubscriptionQualifierValue(policy);
                eventName = String.format("%s%s%s=%s", eventName, FrameworkEvents.EVENT_SEP, key, value);
                break;
            case RAW:
                break;
            }

            filteredEventNames.add(eventName);

            if (eventName.startsWith("resource.change;account=") && policy.getClusterId() != null) {
                eventName = String.format("resource.change;cluster=%d", policy.getClusterId());
                filteredEventNames.add(eventName);
            }
        }

        if (SubscriptionStyle.QUALIFIED.equals(style)) {
            // Subscribe as user
            String eventName = String.format("%s%s%s=%d", EVENT_DISCONNECT, FrameworkEvents.EVENT_SEP,
                    FrameworkEvents.ACCOUNT_QUALIFIER, policy.getAuthenticatedAsAccountId());
            filteredEventNames.add(eventName);

            // Subscribe as project
            if (policy.getAccountId() != policy.getAuthenticatedAsAccountId()) {
                eventName = String.format("%s%s%s=%d", EVENT_DISCONNECT, FrameworkEvents.EVENT_SEP,
                        FrameworkEvents.ACCOUNT_QUALIFIER, policy.getAccountId());
                filteredEventNames.add(eventName);
            }
        }
        request.setResponseContentType("text/plain");

        try {
            for (SubscriptionHandler handler : handlers) {
                if (handler.subscribe(filteredEventNames, request, style != SubscriptionStyle.RAW)) {
                    return new Object();
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to subscribe to [" + filteredEventNames + "]", e);
        }

        return null;
    }

    protected List<String> getEventNames(ApiRequest request) {
        Subscribe subscribe = request.proxyRequestObject(Subscribe.class);

        List<String> eventNames = subscribe.getEventNames();
        if (eventNames != null) {
            return eventNames;
        }

        eventNames = new ArrayList<>();
        Map<String, List<Condition>> conditions = request.getConditions();
        if (conditions == null) {
            return eventNames;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        List<?> list = ProxyUtils.proxy((Map) conditions, Subscribe.class).getEventNames();
        if (list != null) {
            for (Object condition : list) {
                if (condition instanceof Condition) {
                    Object value = ((Condition) condition).getValue();
                    if (value != null) {
                        eventNames.add(value.toString());
                    }
                }
            }
        }

        return eventNames;
    }

    @Override
    public Object listSupport(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        return create(type, ApiContext.getContext().getApiRequest());
    }

}
