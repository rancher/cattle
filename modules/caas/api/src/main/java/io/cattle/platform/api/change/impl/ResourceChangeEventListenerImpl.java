package io.cattle.platform.api.change.impl;

import io.cattle.platform.api.change.ResourceChangeEventListener;
import io.cattle.platform.core.dao.ClusterDao;
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
import io.cattle.platform.util.type.CollectionUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static io.cattle.platform.core.model.tables.AgentTable.*;

public class ResourceChangeEventListenerImpl implements ResourceChangeEventListener, Task, TaskOptions {

    volatile Set<Change> changed = Collections.newSetFromMap(new ConcurrentHashMap<>());
    LockDelegator lockDelegator;
    EventService eventService;
    ObjectManager objectManager;
    JsonMapper jsonMapper;
    ClusterDao clusterDao;

    public ResourceChangeEventListenerImpl(LockDelegator lockDelegator, EventService eventService, ObjectManager objectManager, JsonMapper jsonMapper, ClusterDao clusterDao) {
        super();
        this.lockDelegator = lockDelegator;
        this.eventService = eventService;
        this.objectManager = objectManager;
        this.jsonMapper = jsonMapper;
        this.clusterDao = clusterDao;
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
        Map<String, Object> data = CollectionUtils.toMap(event.getData());
        Long accountId = toLong(data.get(ObjectMetaDataManager.ACCOUNT_FIELD));
        Long clusterId = toLong(data.get(ObjectMetaDataManager.CLUSTER_FIELD));

        if (type != null && id != null) {
            changed.add(new Change(type, id, accountId, clusterId));
        }
    }

    private Long toLong(Object obj) {
        if (obj instanceof Long) {
            return (Long) obj;
        } else if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        return null;
    }

    @Override
    public void run() {
        if (!lockDelegator.tryLock(new ResourceChangePublishLock())) {
            changed.clear();
            return;
        }

        Set<Change> changed = this.changed;
        this.changed = Collections.newSetFromMap(new ConcurrentHashMap<>());

        for (Change change : changed) {
            eventService.publish(EventVO.newEvent(FrameworkEvents.RESOURCE_CHANGE)
                    .withResourceType(change.type)
                    .withResourceId(Objects.toString(change.id, null)));

            if (change.accountId != null) {
                String event = FrameworkEvents.appendAccount(FrameworkEvents.RESOURCE_CHANGE, change.accountId);
                eventService.publish(EventVO.newEvent(event)
                        .withResourceType(change.type)
                        .withResourceId(Objects.toString(change.id, null)));

                Long systemAccountId = clusterDao.getOwnerAcccountIdForCluster(change.clusterId);
                if (systemAccountId != null && !Objects.equals(systemAccountId, change.accountId)) {
                    event = FrameworkEvents.appendAccount(FrameworkEvents.RESOURCE_CHANGE, systemAccountId);
                    eventService.publish(EventVO.newEvent(event)
                            .withResourceType(change.type)
                            .withResourceId(Objects.toString(change.id, null)));
                }
            } else if (change.clusterId != null) {
                String event = FrameworkEvents.appendCluster(FrameworkEvents.RESOURCE_CHANGE, change.clusterId);
                eventService.publish(EventVO.newEvent(event)
                        .withResourceType(change.type)
                        .withResourceId(Objects.toString(change.id, null)));
            }
        }
    }

    @Override
    public String getName() {
        return "resource.change.publish";
    }

    @Override
    public boolean isShouldLock() {
        return false;
    }

    private static class Change {
        String type;
        String id;
        Long accountId;
        Long clusterId;

        public Change(String type, String id, Long accountId, Long clusterId) {
            this.type = type;
            this.id = id;
            this.accountId = accountId;
            this.clusterId = clusterId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Change change = (Change) o;

            if (type != null ? !type.equals(change.type) : change.type != null) return false;
            if (id != null ? !id.equals(change.id) : change.id != null) return false;
            if (accountId != null ? !accountId.equals(change.accountId) : change.accountId != null) return false;
            return clusterId != null ? clusterId.equals(change.clusterId) : change.clusterId == null;
        }

        @Override
        public int hashCode() {
            int result = type != null ? type.hashCode() : 0;
            result = 31 * result + (id != null ? id.hashCode() : 0);
            result = 31 * result + (accountId != null ? accountId.hashCode() : 0);
            result = 31 * result + (clusterId != null ? clusterId.hashCode() : 0);
            return result;
        }
    }

}
