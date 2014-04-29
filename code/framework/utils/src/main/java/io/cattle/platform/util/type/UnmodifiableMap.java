package io.cattle.platform.util.type;

import java.util.Map;

public interface UnmodifiableMap<K,V> extends Map<K, V> {

    Map<K,V> getModifiableCopy();

}