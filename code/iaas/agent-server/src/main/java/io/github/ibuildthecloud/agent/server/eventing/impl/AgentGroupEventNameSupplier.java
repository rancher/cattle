package io.github.ibuildthecloud.agent.server.eventing.impl;

import io.github.ibuildthecloud.agent.server.group.AgentGroupManager;
import io.github.ibuildthecloud.agent.server.group.AgentGroupManagerProvider;
import io.github.ibuildthecloud.dstack.eventing.annotation.AnnotatedEventListener;
import io.github.ibuildthecloud.dstack.eventing.annotation.EventHandler;
import io.github.ibuildthecloud.dstack.eventing.annotation.EventNameProvider;
import io.github.ibuildthecloud.dstack.eventing.util.EventUtils;
import io.github.ibuildthecloud.dstack.iaas.event.IaasEvents;

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
