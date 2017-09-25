package io.cattle.platform.condition;

import java.util.function.BiConsumer;

public interface ConditionValues<T> {

    String getKey(T obj);

    void setValueSetters(BiConsumer<String, Boolean> valueSetters);

    boolean loadInitialValue(T obj);

}
