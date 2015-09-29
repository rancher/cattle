package io.cattle.platform.redis;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.pool.PoolConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import com.google.common.util.concurrent.SettableFuture;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicStringProperty;

public class RedisConnection extends ManagedContextRunnable implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(RedisConnection.class);

    private static final DynamicLongProperty REDIS_RETRY = ArchaiusUtil.getLong("redis.retry.millis");
    private static final DynamicStringProperty REDIS_PASSWORD = ArchaiusUtil.getString("redis.password");
    private static final DynamicIntProperty REDIS_TIMEOUT = ArchaiusUtil.getInt("redis.timeout");

    /* Not actually static, but I just liked the look of the capital case */
    private final Object CONNECTION_LOCK = new Object();

    RedisEventingService eventService;
    Set<String> subscriptions = Collections.synchronizedSet(new HashSet<String>());
    Map<String, SettableFuture<?>> futures = Collections.synchronizedMap(new HashMap<String, SettableFuture<?>>());
    Jedis jedis;
    JedisPubSub pubSub;
    JedisPool pool;
    String host;
    int port;
    volatile boolean shutdown = false;

    public RedisConnection(RedisEventingService eventService, String host, int port, Set<String> subscriptions) {
        super();
        this.host = host;
        this.port = port;
        this.eventService = eventService;
        this.subscriptions.addAll(subscriptions);

        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        PoolConfig.setConfig(config, "redis", "redis.pool.", "global.pool.");

        pool = new JedisPool(config, host, port, REDIS_TIMEOUT.get(), getPassword());
        jedis = new Jedis(host, port, REDIS_TIMEOUT.get());
    }

    public void subscribe(String name, SettableFuture<?> future) {
        futures.put(name, future);
        boolean connected = false;
        synchronized (CONNECTION_LOCK) {
            subscriptions.add(name);
            if (pubSub == null) {
                subscriptions.add(name);
                future.set(null);
                futures.remove(name);
            } else {
                if (waitForConnected(name, future)) {
                    connected = true;
                    pubSub.psubscribe(name);
                }
            }
        }
        if (!connected) {
            tryConnect();
        }
    }

    public boolean publish(String channel, String message) {
        Jedis current = null;
        try {
            current = pool.getResource();
        } catch (Throwable t) {
            log.debug("Failed to get connection from the pool [{}]", host);
            return false;
        }
        try {
            current.publish(channel, message);
            return true;
        } catch (Throwable t) {
            log.error("Failed to publish message [{}] to [{}]", message, channel, t);
            pool.returnBrokenResource(current);
            current = null;
            return false;
        } finally {
            if (current != null) {
                pool.returnResource(current);
            }
        }
    }

    protected boolean waitForConnected(String name, SettableFuture<?> future) {
        long start = System.currentTimeMillis();

        while (pubSub == null || pubSub.getSubscribedChannels() == 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                if (future != null) {
                    futures.remove(name);
                    future.setException(e);
                }
                return false;
            }

            if (System.currentTimeMillis() - start > (REDIS_TIMEOUT.get() * 3)) {
                if (future != null) {
                    futures.remove(name);
                    future.setException(new TimeoutException("Failed to connect to redis"));
                }
                return false;
            }
        }

        return true;
    }

    public void unsubscribe(String name) {
        synchronized (CONNECTION_LOCK) {
            subscriptions.remove(name);
            futures.remove(name);
            if (pubSub != null) {
                if (waitForConnected(null, null)) {
                    pubSub.punsubscribe(name);
                }
            }
        }
    }

    protected void onSubscribed(String name) {
        SettableFuture<?> future = futures.remove(name);
        if (future != null) {
            future.set(null);
        }
    }

    public void stop() {
        shutdown = true;
        pool.destroy();
        jedis.disconnect();
    }

    public void disconnect() {
        jedis.disconnect();
    }

    protected String getPassword() {
        String password = REDIS_PASSWORD.get();
        return StringUtils.isEmpty(password) ? null : password;
    }

    protected void tryConnect() {
        synchronized (CONNECTION_LOCK) {
            CONNECTION_LOCK.notifyAll();
        }
    }

    @Override
    protected void runInContext() {
        while (!shutdown) {
            try {
                synchronized (CONNECTION_LOCK) {
                    jedis.disconnect();
                    pubSub = null;
                }

                if (subscriptions.size() > 0) {
                    log.info("Connecting to redis [{}:{}]", host, port);
                    jedis.getClient().setPassword(getPassword());
                    jedis.connect();

                    String[] subs = null;

                    synchronized (CONNECTION_LOCK) {
                        subs = subscriptions.toArray(new String[subscriptions.size()]);
                        pubSub = new RedisPubSub(eventService, this);
                    }

                    jedis.psubscribe(pubSub, subs);
                }
            } catch (Throwable t) {
                if (!shutdown)
                    log.error("Jedis Exception", t);
            }

            synchronized (CONNECTION_LOCK) {
                pubSub = null;
                jedis.disconnect();
            }

            try {
                synchronized (CONNECTION_LOCK) {
                    CONNECTION_LOCK.wait(REDIS_RETRY.get());
                }
            } catch (InterruptedException e) {
                log.error("Interrupted", e);
            }
        }

        synchronized (CONNECTION_LOCK) {
            pubSub = null;
            pool.destroy();
            jedis.disconnect();
        }
    }

}
