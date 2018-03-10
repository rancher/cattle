package io.cattle.platform.hazelcast.eventing;

import io.cattle.platform.eventing.impl.AbstractThreadPoolingEventService;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.util.type.Named;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.cloudstack.managed.context.NoExceptionRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.SettableFuture;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import com.hazelcast.spi.AbstractDistributedObject;
import com.hazelcast.spi.EventService;
import com.hazelcast.spi.NodeEngine;
import com.hazelcast.topic.impl.TopicService;

public class HazelcastEventService extends AbstractThreadPoolingEventService implements Named {

    private static final Logger log = LoggerFactory.getLogger(HazelcastEventService.class);
    private static final long DELETE_INTERVAL = 15L;
    private static final int DELETE_COUNT = 3;

    ScheduledExecutorService scheduledExecutorService;
    HazelcastInstance hazelcast;
    Map<String, String> registrations = new ConcurrentHashMap<>();
    Map<String, Integer> toDelete = new ConcurrentHashMap<>();

    @Override
    public void init() {
        super.init();
        scheduledExecutorService.scheduleWithFixedDelay(new NoExceptionRunnable() {
            @Override
            protected void doRun() throws Exception {
                processDeletes();
            }
        }, DELETE_INTERVAL, DELETE_INTERVAL, TimeUnit.SECONDS);
    }

    @Override
    protected boolean doPublish(String name, Event event, String eventString) throws IOException {
        ITopic<String> topic = hazelcast.getTopic(name);
        topic.publish(eventString);

        return true;
    }

    @Override
    protected synchronized void doSubscribe(final String eventName, SettableFuture<?> future) {
        boolean success = false;
        Throwable t = null;
        try {
            if (registrations.containsKey(eventName)) {
                throw new IllegalStateException("Already subscribed to [" + eventName + "]");
            }

            ITopic<String> topic = hazelcast.getTopic(eventName);
            MessageListener<String> listener = new MessageListener<String>() {
                @Override
                public void onMessage(Message<String> message) {
                    String eventString = message.getMessageObject();
                    if (eventString != null) {
                        onEvent(null, eventName, eventString);
                    }
                }
            };

            String id = topic.addMessageListener(listener);
            log.info("Subscribing to [{}] id [{}]", eventName, id);

            registrations.put(eventName, id);

            success = true;
        } catch (RuntimeException e) {
            t = e;
            throw e;
        } finally {
            if (success) {
                future.set(null);
            } else {
                if (t == null) {
                    t = new IllegalStateException("Failed to subscribe to [" + eventName + "]");
                }
                future.setException(t);
            }
        }
    }

    protected synchronized void processDeletes() {
        Map<String, Integer> newToDelete = new HashMap<>();

        for (Map.Entry<String, Integer> entry : toDelete.entrySet()) {
            String topicName = entry.getKey();
            int count = entry.getValue();
            if (registrations.containsKey(topicName)) {
                continue;
            }

            ITopic<String> topic = hazelcast.getTopic(topicName);
            if (!(topic instanceof AbstractDistributedObject)) {
                continue;
            }

            NodeEngine engine = ((AbstractDistributedObject<?>)topic).getNodeEngine();
            EventService eventService = engine.getEventService();
            if (eventService.getRegistrations(TopicService.SERVICE_NAME, topicName).size() != 0) {
                continue;
            }

            count++;
            if (count > DELETE_COUNT) {
                log.info("Removing hazelcast topic [{}]", topicName);
                try {
                    topic.destroy();
                } catch (Throwable t) {
                   // Ignore
                }
            } else {
                newToDelete.put(topicName, count);
            }
        }

        toDelete = newToDelete;
    }

    @Override
    protected synchronized void doUnsubscribe(String eventName) {
        String id = registrations.remove(eventName);
        log.info("Unsubscribing from [{}] id [{}]", eventName, id);

        if (id != null) {
            ITopic<String> topic = hazelcast.getTopic(eventName);
            topic.removeMessageListener(id);
            toDelete.put(eventName, 0);
        }
    }

    @Override
    protected void disconnect() {
    }

    public HazelcastInstance getHazelcast() {
        return hazelcast;
    }

    @Inject
    public void setHazelcast(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
    }

    @Override
    public String getName() {
        return "EventService";
    }

    public ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }

    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
    }

}
