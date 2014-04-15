package io.cattle.platform.object.util;

import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.ConvertUtils;

public class DataAccessor {

    Object source;
    Object defaultValue;
    Map<String,Object> sourceMap;
    String key;
    Class<?> scope;
    String scopeKey;

    public static DataAccessor fromDataFieldOf(Object obj) {
        DataAccessor accessor = new DataAccessor();
        accessor.source = obj;
        return accessor;
    }

    public static DataAccessor fromMap(Object obj) {
        DataAccessor accessor = new DataAccessor();
        accessor.sourceMap = CollectionUtils.castMap(obj);
        return accessor;
    }

    public static DataAccessor fields(Object obj) {
        DataAccessor accessor = fromDataFieldOf(obj);
        accessor.scopeKey = DataUtils.FIELDS;

        return accessor;
    }

    public static <T> T field(Object obj, String name, JsonMapper mapper, Class<T> type) {
        return fields(obj)
                .withKey(name)
                .as(mapper, type);
    }

    public static <T> T field(Object obj, String name, Class<T> type) {
        return fields(obj)
                .withKey(name)
                .as(type);
    }

    public DataAccessor withScope(Class<?> scope) {
        this.scope = scope;
        return this;
    }

    public DataAccessor withScopeKey(String scopeKey) {
        this.scopeKey = scopeKey;
        return this;
    }

    public DataAccessor withKey(String key) {
        this.key = key;
        return this;
    }

    public DataAccessor withDefault(Object defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public <T> T as(JsonMapper mapper, Class<T> clz) {
        return mapper.convertValue(get(), clz);
    }

    @SuppressWarnings("unchecked")
    public <T> T as(Class<T> clz) {
        return (T)ConvertUtils.convert(get(), clz);
    }

    public Object get() {
        Map<String,Object> map = getTargetMap(false);
        Object result = key == null ? null : map.get(key);
        return result == null ? defaultValue : result;
    }

    public void set(Object value) {
        Map<String,Object> map = getTargetMap(true);
        if ( key != null ) {
            map.put(key, value);
        }

        if ( source != null ) {
            Map<String,Object> data = DataUtils.getData(source);
            DataUtils.setData(source, data);
        }
    }

    public void remove() {
        Map<String,Object> map = getTargetMap(true);
        if ( key != null ) {
            map.remove(key);
        }

        if ( source != null ) {
            Map<String,Object> data = DataUtils.getData(source);
            DataUtils.setData(source, data);
        }
    }

    protected Map<String,Object> getTargetMap(boolean addContainer) {
        if ( sourceMap == null && source != null ) {
            sourceMap = DataUtils.getData(source);
        }

        if ( sourceMap == null ) {
            if ( ! addContainer ) {
                throw new IllegalStateException("Can not set a value on a null target");
            }
            return null;
        }

        Map<String,Object> map = sourceMap;

        if ( isScopeSet() ) {
            Object scopedMap = sourceMap.get(getScope());
            if ( scopedMap == null && addContainer ) {
                scopedMap = new HashMap<String,Object>();
                sourceMap.put(getScope(), scopedMap);
            }
            map = CollectionUtils.castMap(scopedMap);
        }

        return map;
    }

    protected boolean isScopeSet() {
        return scope != null || scopeKey != null;
    }

    protected String getScope() {
        return scope == null ? scopeKey : scope.getName();
    }
}