package io.github.ibuildthecloud.api.pubsub.manager;

import io.github.ibuildthecloud.api.pubsub.model.Subscribe;
import io.github.ibuildthecloud.api.pubsub.subscribe.SubscriptionHandler;
import io.github.ibuildthecloud.api.pubsub.util.SubscriptionUtils;
import io.github.ibuildthecloud.api.pubsub.util.SubscriptionUtils.SubscriptionStyle;
import io.github.ibuildthecloud.dstack.api.auth.Policy;
import io.github.ibuildthecloud.dstack.api.utils.ApiUtils;
import io.github.ibuildthecloud.dstack.framework.event.FrameworkEvents;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;
import io.github.ibuildthecloud.gdapi.util.ProxyUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class SubscribeManager extends AbstractNoOpResourceManager {

    List<SubscriptionHandler> handlers = new ArrayList<SubscriptionHandler>();

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Subscribe.class };
    }

    @Override
    protected Object createInternal(String type, ApiRequest request) {
        List<String> eventNames = getEventNames(request);
        List<String> filteredEventNames = new ArrayList<String>(eventNames.size());

        Policy policy = ApiUtils.getPolicy();

        SubscriptionStyle style = SubscriptionUtils.getSubscriptionStyle(policy);
        for ( String eventName : eventNames ) {
            switch (style) {
            case QUALIFIED:
                if ( eventName.contains(FrameworkEvents.EVENT_SEP) ) {
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
        }

        if ( filteredEventNames.size() == 0 ) {
            return null;
        }

        request.setResponseContentType("text/plain");

        try {
            for ( SubscriptionHandler handler : handlers ) {
                if ( handler.subscribe(filteredEventNames, request, style != SubscriptionStyle.RAW) ) {
                    return new Object();
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to subscribe to [" + filteredEventNames + "]", e);
        }

        return super.createInternal(type, request);
    }

    protected List<String> getEventNames(ApiRequest request) {
        Subscribe subscribe = request.proxyRequestObject(Subscribe.class);

        List<String> eventNames = subscribe.getEventNames();
        if ( eventNames != null ) {
            return eventNames;
        }

        eventNames = new ArrayList<String>();
        Map<String,List<Condition>> conditions = request.getConditions();
        if ( conditions == null ) {
            return eventNames;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        List<?> list = ProxyUtils.proxy((Map)conditions, Subscribe.class).getEventNames();
        if ( list != null ) {
            for ( Object condition : list ) {
                if ( condition instanceof Condition ) {
                    Object value = ((Condition)condition).getValue();
                    if ( value != null ) {
                        eventNames.add(value.toString());
                    }
                }
            }
        }

        return eventNames;
    }

    @Override
    protected Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        return createInternal(type, ApiContext.getContext().getApiRequest());
    }

    public List<SubscriptionHandler> getHandlers() {
        return handlers;
    }

    @Inject
    public void setHandlers(List<SubscriptionHandler> handlers) {
        this.handlers = handlers;
    }

}
