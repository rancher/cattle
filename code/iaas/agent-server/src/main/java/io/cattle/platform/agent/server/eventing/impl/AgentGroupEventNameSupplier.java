package io.cattle.platform.agent.server.eventing.impl;

import io.cattle.platform.agent.server.group.AgentGroupManager;
import io.cattle.platform.agent.server.group.AgentGroupManagerProvider;
import io.cattle.platform.eventing.annotation.AnnotatedEventListener;
import io.cattle.platform.eventing.annotation.EventHandler;
import io.cattle.platform.eventing.annotation.EventNameProvider;
import io.cattle.platform.eventing.util.EventUtils;
import io.cattle.platform.iaas.event.IaasEvents;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class AgentGroupEventNameSupplier implements EventNameProvider {

    @Override
    public List<String> events(EventHandler eventHandler, AnnotatedEventListener listener, Method method) {
        if ( ! (listener instanceof AgentGroupManagerProvider) ) {
            throw new IllegalArgumentException("Listener [" + listener + "] must implement AgentGroupManagerProvider");
        }

        AgentGroupManager manager = ((AgentGroupManagerProvider)listener).getAgentGroupManager();

        String eventName = EventUtils.getEventNameNonProvided(eventHandler, listener, method);
        List<String> events = new ArrayList<String>();

        if ( manager.shouldHandleUnassigned() || manager.shouldHandleWildcard() ) {
            events.add(eventName);
        }

        for ( Long groupId : manager.supportedGroups() ) {
            events.add(IaasEvents.appendAgentGroup(eventName, groupId));
        }

        return events;
    }

}
