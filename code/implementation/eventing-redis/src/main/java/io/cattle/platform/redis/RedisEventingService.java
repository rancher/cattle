package io.cattle.platform.redis;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.eventing.impl.AbstractThreadPoolingEventService;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.util.type.InitializationTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import redis.clients.jedis.Protocol;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.netflix.config.DynamicStringProperty;

public class RedisEventingService extends AbstractThreadPoolingEventService implements InitializationTask {

    private static final DynamicStringProperty REDIS_HOST = ArchaiusUtil.getString("redis.hosts");

    volatile List<RedisConnection> connections = new ArrayList<RedisConnection>();
    int index = 0;

    public void reconnect() {
        List<RedisConnection> newConnections = new ArrayList<RedisConnection>();

        for ( String host : REDIS_HOST.get().trim().split("\\s*,\\s*") ) {
            String[] parts = host.split(":");
            if ( parts.length > 2 ) {
                throw new IllegalArgumentException("Invalid redis host [" + host + "] should be in host:port format");
            }

            String hostName = parts[0];
            int port = Protocol.DEFAULT_PORT;

            if ( parts.length > 1 ) {
                try {
                    port = Integer.parseInt(parts[1]);
                } catch ( NumberFormatException e ) {
                    throw new IllegalArgumentException("Invalid redis host [" + host + "] should be in host:port format", e);
                }
            }

            newConnections.add(new RedisConnection(this, hostName, port));
        }

        List<RedisConnection> oldConnections = connections;
        connections = newConnections;

        for ( RedisConnection conn : newConnections ) {
            getExecutorService().submit(conn);
        }

        for ( RedisConnection conn : oldConnections ) {
            conn.stop();
        }
    }

    @Override
    public void start() {
        super.start();

        reconnect();

        REDIS_HOST.addCallback(new Runnable() {
            @Override
            public void run() {
                reconnect();
            }
        });
    }

    @Override
    public void stop() {
        super.stop();

        for ( RedisConnection conn : connections ) {
            conn.stop();
        }
    }

    protected void onMessage(String pattern, String channel, String message) {
        onEvent(pattern, channel, message);
    }

    @Override
    protected boolean doPublish(String name, Event event, String eventString) throws IOException {
        if ( connections.size() == 0 ) {
            return false;
        }

        RedisConnection conn = null;
        try {
            conn = connections.get(index);
        } catch ( IndexOutOfBoundsException e ) {
            if ( connections.size() > 0 ) {
                conn = connections.get(0);
            }
        }

        index = (index+1) % connections.size();

        if ( conn != null ) {
            return conn.publish(name, eventString);
        }

        return false;
    }

    @Override
    protected void doSubscribe(String eventName, final SettableFuture<?> future) {
        List<SettableFuture<?>> futures = new ArrayList<SettableFuture<?>>();

        for ( RedisConnection conn : connections ) {
            SettableFuture<?> newFuture = SettableFuture.create();
            conn.subscribe(eventName, newFuture);
            futures.add(newFuture);
        }

        final ListenableFuture<?> combined = Futures.allAsList(futures);

        combined.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    combined.get();
                    future.set(null);
                } catch (InterruptedException e) {
                    future.setException(e);
                } catch (ExecutionException e) {
                    future.setException(e.getCause());
                }
            }
        }, getExecutorService());
    }

    @Override
    protected void doUnsubscribe(String eventName) {
        for ( RedisConnection conn : connections ) {
            conn.unsubscribe(eventName);
        }
    }

    @Override
    protected void disconnect() {
        for ( RedisConnection conn : connections ) {
            conn.disconnect();
        }
    }


}
