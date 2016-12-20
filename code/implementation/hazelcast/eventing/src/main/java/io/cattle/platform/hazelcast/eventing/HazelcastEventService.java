package io.cattle.platform.hazelcast.eventing;

import io.cattle.platform.eventing.impl.AbstractThreadPoolingEventService;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.util.type.Named;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.SettableFuture;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

public class HazelcastEventService extends AbstractThreadPoolingEventService implements Named {

    private static final Logger log = LoggerFactory.getLogger(HazelcastEventService.class);

    HazelcastInstance hazelcast;
    Map<String, String> registrations = new ConcurrentHashMap<String, String>();

    @Override
    protected boolean doPublish(String name, Event event, String eventString) throws IOException {
        ITopic<String> topic = hazelcast.getTopic(name);
        topic.publish(eventString);

        return true;
    }

    @Override
    protected void doSubscribe(final String eventName, SettableFuture<?> future) {
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
                    onEvent(null, eventName, message.getMessageObject());
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

    @Override
    protected void doUnsubscribe(String eventName) {
        String id = registrations.remove(eventName);
        log.info("Unsubscribing from [{}] id [{}]", eventName, id);

        if (id != null) {
            ITopic<String> topic = hazelcast.getTopic(eventName);
            topic.removeMessageListener(id);
            if (eventName.startsWith("reply.")) {
                topic.destroy();
            }
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

}
