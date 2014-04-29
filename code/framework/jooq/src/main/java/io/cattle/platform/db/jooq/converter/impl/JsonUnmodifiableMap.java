package io.cattle.platform.db.jooq.converter.impl;

import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.util.type.UnmodifiableMap;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class JsonUnmodifiableMap<K,V> implements UnmodifiableMap<K, V> {

    Map<K,V> map;
    JsonMapper jsonMapper;
    String text;

    @SuppressWarnings("unchecked")
    public JsonUnmodifiableMap(JsonMapper mapper, String text) throws IOException {
        this.map = (Map<K, V>)Collections.unmodifiableMap(mapper.readValue(text));
        this.jsonMapper = mapper;
        this.text = text;
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
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        return map.equals(o);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public String toString() {
        return map.toString();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<K, V> getModifiableCopy() {
        try {
            return (Map<K, V>)jsonMapper.readValue(text);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}