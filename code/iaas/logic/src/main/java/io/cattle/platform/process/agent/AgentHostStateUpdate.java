package io.cattle.platform.process.agent;

import static io.cattle.platform.core.model.tables.HostTable.*;

import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessDefinition;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.StateTransition;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.framework.event.FrameworkEvents;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentHostStateUpdate {

    private static final Logger log = LoggerFactory.getLogger(AgentHostStateUpdate.class);

    SchemaFactory schemaFactory;
    Map<String, ProcessDefinition> processDefinitions;
    EventService eventService;
    ObjectManager objectManager;

    Map<String, String> transitioningToDone = new HashMap<>();

    public AgentHostStateUpdate(SchemaFactory schemaFactory, Map<String, ProcessDefinition> processDefinitions, EventService eventService,
            ObjectManager objectManager) {
        super();
        this.schemaFactory = schemaFactory;
        this.processDefinitions = processDefinitions;
        this.eventService = eventService;
        this.objectManager = objectManager;
        init();
    }

    public HandlerResult preHandle(ProcessState state, ProcessInstance process) {
        for (Host host : objectManager.children(state.getResource(), Host.class)) {
            log.debug("Setting host [{}] agentState to [{}] on pre", host.getId(), state.getState());
            setState(host, state.getState());
        }

        return null;
    }

    public HandlerResult postHandle(ProcessState state, ProcessInstance process) {
        Agent agent = (Agent)state.getResource();
        String newState = transitioningToDone.get(state.getState());

        if (newState == null) {
            return null;
        }

        for (Host host : objectManager.children(agent, Host.class)) {
            log.debug("Setting host [{}] agentState to [{}] on post", host.getId(), newState);
            setState(host, newState);
        }

        return null;
    }

    protected void setState(Host host, String newState) {
        Map<Object, Object> props = new HashMap<>();
        props.put(HOST.AGENT_STATE, newState);

        Account account = objectManager.loadResource(Account.class, host.getAccountId());
        Long delay = DataAccessor.fieldLong(account, AccountConstants.FIELD_HOST_REMOVE_DELAY);
        if (delay == null && HostDao.HOST_REMOVE_DELAY.get() > -1) {
            delay = HostDao.HOST_REMOVE_DELAY.get();
        }
        if (delay != null && delay > -1) {
            props.put(HOST.REMOVE_AFTER, new Date(System.currentTimeMillis() + delay * 1000));
        } else {
            props.put(HOST.REMOVE_AFTER, null);
        }
        objectManager.setFields(host, objectManager.convertToPropertiesFor(host, props));
        trigger(host);
    }

    protected void trigger(Host host) {
        Map<String, Object> data = new HashMap<>();
        data.put(ObjectMetaDataManager.ACCOUNT_FIELD, host.getAccountId());

        Event event = EventVO.newEvent(FrameworkEvents.STATE_CHANGE)
                .withData(data)
                .withResourceType(HostConstants.TYPE)
                .withResourceId(host.getId().toString());

        eventService.publish(event);
    }

    private void init() {
        String type = schemaFactory.getSchemaName(Agent.class);
        if (type == null) {
            return;
        }

        for (ProcessDefinition def : processDefinitions.values()) {
            if (!type.equals(def.getResourceType())) {
                continue;
            }

            for (StateTransition transition : def.getStateTransitions()) {
                if (transition.getType() == StateTransition.Style.DONE) {
                    transitioningToDone.put(transition.getFromState(), transition.getToState());
                }
            }
        }
    }

}
