package io.cattle.platform.db.jooq.converter.impl;

import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.util.type.UnmodifiableMap;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonUnmodifiableMap<K, V> implements UnmodifiableMap<K, V> {

    private static final Logger log = LoggerFactory.getLogger(JsonUnmodifiableMap.class);

    Map<K, V> map;
    JsonMapper jsonMapper;
    String text;
    boolean writeable;

    protected JsonUnmodifiableMap(JsonUnmodifiableMap<K, V> map) {
        this.jsonMapper = map.jsonMapper;
        this.text = map.text;
        if (map.writeable) {
            this.map = map.map;
        }
        this.writeable = true;
    }

    public JsonUnmodifiableMap(JsonMapper mapper, String text) {
        this.jsonMapper = mapper;
        this.text = text;
    }

    @Override
    public int size() {
        return getMap().size();
    }

    @Override
    public boolean isEmpty() {
        return getMap().isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return getMap().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return getMap().containsValue(value);
    }

    @Override
    public V get(Object key) {
        return getMap().get(key);
    }

    @Override
    public V put(K key, V value) {
        return getMap().put(key, value);
    }

    @Override
    public V remove(Object key) {
        return getMap().remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        getMap().putAll(m);
    }

    @Override
    public void clear() {
        getMap().clear();
    }

    @Override
    public Set<K> keySet() {
        return getMap().keySet();
    }

    @Override
    public Collection<V> values() {
        return getMap().values();
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        return getMap().entrySet();
    }

    @Override
    public boolean equals(Object o) {
        return getMap().equals(o);
    }

    @Override
    public int hashCode() {
        return getMap().hashCode();
    }

    @Override
    public String toString() {
        return getMap().toString();
    }

    public void setText(String text) {
        this.text = text;
        this.map = null;
        this.writeable = false;
    }

    @SuppressWarnings("unchecked")
    protected  Map<K, V> getMap() {
        if (this.map == null) {
            try {
                this.map = (Map<K, V>) jsonMapper.readValue(text);
                if (!writeable) {
                    this.map = (Map<K, V>) Collections.unmodifiableMap(this.map);
                }
            } catch (IOException e) {
                log.error("Failed to unmarshall {}", text, e);
                this.map = (Map<K, V>)Collections.unmodifiableMap(new HashMap<>());
            }
        }
        return this.map;
    }

    @Override
    public Map<K, V> getModifiableCopy() {
        return new JsonUnmodifiableMap<K, V>(this);
    }
}