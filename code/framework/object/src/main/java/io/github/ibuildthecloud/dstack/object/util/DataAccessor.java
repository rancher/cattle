package io.github.ibuildthecloud.dstack.object.util;

import io.github.ibuildthecloud.dstack.json.JsonMapper;
import io.github.ibuildthecloud.dstack.util.type.CollectionUtils;

import java.util.HashMap;
import java.util.Map;

public class DataAccessor {

    Object source;
    Map<String,Object> sourceMap;
    String key;
    Class<?> scope;

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

    public DataAccessor withScope(Class<?> scope) {
        this.scope = scope;
        return this;
    }

    public DataAccessor withKey(String key) {
        this.key = key;
        return this;
    }

    public <T> T as(JsonMapper mapper, Class<T> clz) {
        return mapper.convertValue(get(), clz);
    }

    public Object get() {
        Map<String,Object> map = getTargetMap(false);
        return key == null ? null : map.get(key);
    }

    public void set(Object value) {
        Map<String,Object> map = getTargetMap(true);
        if ( key != null ) {
            map.put(key, value);
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

        if ( scope != null ) {
            Object scopedMap = sourceMap.get(scope.getName());
            if ( scopedMap == null && addContainer ) {
                scopedMap = new HashMap<String,Object>();
                sourceMap.put(scope.getName(), scopedMap);
            }
            map = CollectionUtils.castMap(scopedMap);
        }

        return map;
    }
}