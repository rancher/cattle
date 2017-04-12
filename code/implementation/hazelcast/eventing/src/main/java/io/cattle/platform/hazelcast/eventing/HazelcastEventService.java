package io.cattle.platform.hazelcast.eventing;

import io.cattle.platform.eventing.impl.AbstractThreadPoolingEventService;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.util.type.Named;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
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
        TopicName topicName = new TopicName(name);
        ITopic<String> topic = hazelcast.getTopic(topicName.getName());
        topic.publish(topicName.encode(eventString));

        return true;
    }

    @Override
    protected synchronized void doSubscribe(final String eventName, SettableFuture<?> future) {
        TopicName topicName = new TopicName(eventName);
        boolean success = false;
        Throwable t = null;
        try {
            if (registrations.containsKey(eventName)) {
                throw new IllegalStateException("Already subscribed to [" + eventName + "]");
            }

            ITopic<String> topic = hazelcast.getTopic(topicName.getName());
            MessageListener<String> listener = new MessageListener<String>() {
                @Override
                public void onMessage(Message<String> message) {
                    String eventString = topicName.decode(message.getMessageObject());
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

    @Override
    protected synchronized void doUnsubscribe(String eventName) {
        String id = registrations.remove(eventName);
        log.info("Unsubscribing from [{}] id [{}]", eventName, id);

        if (id != null) {
            ITopic<String> topic = hazelcast.getTopic(new TopicName(eventName).getName());
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

    private static class TopicName {
        String name;
        String qualifier;

        public TopicName(String topic) {
            String[] parts = StringUtils.split(topic, ";", 2);
            this.name = parts[0];
            if (parts.length > 1) {
                this.qualifier = parts[1] + ":";
                this.name += ";";
            }
        }

        public String getName() {
            return name;
        }

        public String encode(String eventString) {
            return qualifier == null ? eventString : qualifier + eventString;
        }

        public String decode(String eventString) {
            if (qualifier == null) {
                return eventString;
            }
            if (eventString.startsWith(this.qualifier)) {
                return eventString.substring(this.qualifier.length());
            }
            return null;
        }
    }

}
