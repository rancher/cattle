package io.cattle.platform.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.JedisPubSub;

public class RedisPubSub extends JedisPubSub {

    private static final Logger log = LoggerFactory.getLogger(RedisPubSub.class);

    RedisEventingService service;
    RedisConnection connection;

    public RedisPubSub(RedisEventingService service, RedisConnection connection) {
        this.service = service;
        this.connection = connection;
    }

    @Override
    public void onMessage(String channel, String message) {
        service.onMessage(null, channel, message);
    }

    @Override
    public void onPMessage(String pattern, String channel, String message) {
        service.onMessage(pattern, channel, message);
    }

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {
        log.debug("Subscribed to channel [{}], total subscriptions [{}]", channel, subscribedChannels);
        connection.onSubscribed(channel);
    }

    @Override
    public void onUnsubscribe(String channel, int subscribedChannels) {
        log.debug("Unsubscribed to channel [{}], total subscriptions [{}]", channel, subscribedChannels);
        connection.onSubscribed(channel);
    }

    @Override
    public void onPUnsubscribe(String pattern, int subscribedChannels) {
        log.debug("Unsubscribed to pattern [{}], total subscriptions [{}]", pattern, subscribedChannels);
        connection.onSubscribed(pattern);
    }

    @Override
    public void onPSubscribe(String pattern, int subscribedChannels) {
        log.debug("Subscribed to pattern [{}], total subscriptions [{}]", pattern, subscribedChannels);
        connection.onSubscribed(pattern);
    }

}
