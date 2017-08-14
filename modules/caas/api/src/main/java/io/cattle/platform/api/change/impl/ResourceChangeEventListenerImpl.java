package io.cattle.platform.api.change.impl;

import io.cattle.platform.api.change.ResourceChangeEventListener;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.framework.event.FrameworkEvents;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockDelegator;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.task.Task;
import io.cattle.platform.task.TaskOptions;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.cattle.platform.core.model.tables.AgentTable.*;

public class ResourceChangeEventListenerImpl implements ResourceChangeEventListener, Task, TaskOptions {

    volatile Map<Pair<String, String>, Object> changed = new ConcurrentHashMap<>();
    LockDelegator lockDelegator;
    EventService eventService;
    ObjectManager objectManager;
    JsonMapper jsonMapper;

    public ResourceChangeEventListenerImpl(LockDelegator lockDelegator, EventService eventService, ObjectManager objectManager, JsonMapper jsonMapper) {
        super();
        this.lockDelegator = lockDelegator;
        this.eventService = eventService;
        this.objectManager = objectManager;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void stateChange(Event event) {
        add(event);
    }

    @Override
    public void apiChange(Event event) {
        add(event);
    }

    @Override
    public void resourceProgress(Event event) {
        add(event);
    }

    @Override
    public void serviceEvent(Event event) {
        Account account = objectManager.loadResource(Account.class, event.getResourceId());
        if (account == null) {
            return;
        }

        Agent agent = objectManager.findAny(Agent.class, AGENT.ACCOUNT_ID, new Long(event.getResourceId()));
        if (agent == null) {
            return;
        }

        Long resourceAccId = agent.getResourceAccountId();
        if (resourceAccId == null) {
            return;
        }

        Event originalEvent = jsonMapper.convertValue(event.getData(), EventVO.class);
        EventVO<?, ?> eventWithAccount = new EventVO<>(originalEvent, null);
        eventWithAccount.setName(FrameworkEvents.appendAccount(originalEvent.getName(), resourceAccId));

        eventService.publish(originalEvent);
        eventService.publish(eventWithAccount);
    }

    protected void add(Event event) {
        String id = event.getResourceId();
        String type = event.getResourceType();
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) event.getData();
        Object accountId = data == null ? null : data.get(ObjectMetaDataManager.ACCOUNT_FIELD);
        if (accountId == null) {
            accountId = Boolean.TRUE;
        }

        if (type != null && id != null) {
            changed.put(new ImmutablePair<>(type, id), accountId);
        }
    }

    @Override
    public void run() {
        if (!lockDelegator.tryLock(new ResourceChangePublishLock())) {
            changed.clear();
            return;
        }

        Map<Pair<String, String>, Object> changed = this.changed;
        this.changed = new ConcurrentHashMap<>();

        for (Map.Entry<Pair<String, String>, Object> entry : changed.entrySet()) {
            Pair<String, String> pair = entry.getKey();
            Object accountId = entry.getValue();

            eventService.publish(EventVO.newEvent(FrameworkEvents.RESOURCE_CHANGE).withResourceType(pair.getLeft()).withResourceId(pair.getRight()));

            if (accountId instanceof Number) {
                String event = FrameworkEvents.appendAccount(FrameworkEvents.RESOURCE_CHANGE, ((Number) accountId).longValue());
                eventService.publish(EventVO.newEvent(event).withResourceType(pair.getLeft()).withResourceId(pair.getRight()));
            }
        }
    }

    @Override
    public String getName() {
        return "resource.change.publish";
    }

    @Override
    public boolean isShouldRecord() {
        return false;
    }

    @Override
    public boolean isShouldLock() {
        return false;
    }

}
