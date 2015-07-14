package io.cattle.platform.extension.impl;

import io.cattle.platform.extension.ExtensionManager;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ExtensionMap<K, V> implements Map<K, V> {

    Map<K, V> map;
    ConcurrentHashMap<K, V> inner;
    String key;
    ExtensionManager extensionManager;

    public ExtensionMap(ExtensionManager extensionManager, String key, Map<K, V> map) {
        this.extensionManager = extensionManager;
        this.key = key;
        this.inner = map == null ? new ConcurrentHashMap<K, V>() : new ConcurrentHashMap<>(map);
        this.map = Collections.unmodifiableMap(inner);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return map.get(key);
    }

    @Override
    public V put(K key, V value) {
        return map.put(key, value);
    }

    @Override
    public V remove(Object key) {
        return map.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        map.putAll(m);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return map.entrySet();
    }
}
