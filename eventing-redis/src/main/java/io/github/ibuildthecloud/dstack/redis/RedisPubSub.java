package io.github.ibuildthecloud.dstack.redis;

import redis.clients.jedis.JedisPubSub;

public class RedisPubSub extends JedisPubSub {

    RedisEventingService service;
    RedisConnection connection;

    public RedisPubSub(RedisEventingService service, RedisConnection connection) {
        this.service = service;
        this.connection = connection;
    }

    @Override
    public void onMessage(String channel, String message) {
        service.onMessage(channel, message);
    }

    @Override
    public void onPMessage(String pattern, String channel, String message) {
        service.onMessage(channel, message);
    }

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {
        connection.onSubscribed(channel);
    }

    @Override
    public void onUnsubscribe(String channel, int subscribedChannels) {
        connection.onSubscribed(channel);
    }

    @Override
    public void onPUnsubscribe(String pattern, int subscribedChannels) {
        connection.onSubscribed(pattern);
    }

    @Override
    public void onPSubscribe(String pattern, int subscribedChannels) {
        connection.onSubscribed(pattern);
    }


}
