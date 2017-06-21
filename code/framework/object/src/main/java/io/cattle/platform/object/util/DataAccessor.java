package io.cattle.platform.object.util;

import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.util.type.CollectionUtils;
import io.cattle.platform.util.type.UnmodifiableMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.beanutils.ConvertUtils;

public class DataAccessor {

    Object source;
    Object defaultValue;
    Map<String, Object> sourceMap;
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
        accessor.sourceMap = CollectionUtils.toMap(obj);
        return accessor;
    }

    public static DataAccessor fields(Object obj) {
        DataAccessor accessor = fromDataFieldOf(obj);
        accessor.scopeKey = DataUtils.FIELDS;

        return accessor;
    }

    public static void setField(Object obj, String field, Object fieldValue) {
        DataAccessor.fields(obj).withKey(field).set(fieldValue);
    }

    public static String fieldString(Object obj, String key) {
        return fields(obj).withKey(key).as(String.class);
    }

    public static Map<String, Object> fieldMap(Object obj, String key) {
        Object list = fields(obj).withKey(key).getForWrite();
        return CollectionUtils.toMap(list);
    }

    public static Map<String, Object> fieldMapRO(Object obj, String key) {
        Object list = fields(obj).withKey(key).get();
        return CollectionUtils.toMap(list);
    }

    public static List<String> fieldStringList(Object obj, String key) {
        List<String> result = new ArrayList<>();
        Object list = fields(obj).withKey(key).getForWrite();

        if (list == null || !(list instanceof List)) {
            return result;
        }

        for (Object item : (List<?>) list) {
            result.add(Objects.toString(item, null));
        }

        return result;
    }

    public static List<String> appendToFieldStringList(Object obj, String key, String value) {
        List<String> list = fieldStringList(obj, key);
        if (!list.contains(value)) {
            list.add(value);
        }
        DataAccessor.setField(obj, key, list);
        return list;
    }

    public static Long fieldLong(Object obj, String key) {
        return fields(obj).withKey(key).as(Long.class);
    }

    public static <T> List<T> fieldObjectList(Object obj, String name, Class<T> clz, JsonMapper jsonMapper) {
        Object field = DataAccessor.field(obj, name, Object.class);
        if (field == null) {
            return Collections.emptyList();
        }
        return jsonMapper.convertCollectionValue(field, ArrayList.class, clz);
    }

    public static List<Long> fieldLongList(Object obj, String key) {
        List<Long> result = new ArrayList<>();
        Object list = fields(obj).withKey(key).getForWrite();

        if (list == null || !(list instanceof List)) {
            return result;
        }

        for (Object item : (List<?>) list) {
            if (item == null) {
                result.add(null);
            } else if (item instanceof Number) {
                result.add(((Number) item).longValue());
            } else {
                result.add(Long.parseLong(item.toString()));
            }
        }

        return result;
    }

    public static void setLabel(Object obj, String key, String value) {
        Map<String, Object> labels = fieldMap(obj, "labels");
        labels.put(key, value);
        setField(obj, "labels", labels);
    }

    public static String getLabel(Object obj, String key) {
        return ObjectUtils.toString(fieldMapRO(obj, "labels").get(key));
    }

    public static Integer fieldInteger(Object obj, String key) {
        return fields(obj).withKey(key).as(Integer.class);
    }

    public static Boolean fieldBoolean(Object obj, String key) {
        return fields(obj).withKey(key).as(Boolean.class);
    }

    public static boolean fieldBool(Object obj, String key) {
        return fields(obj).withKey(key).withDefault(false).as(Boolean.class);
    }

    public static Date fieldDate(Object obj, String key) {
        Object val = fields(obj).withKey(key).get();
        if (val instanceof Date) {
            return (Date)val;
        } else if (val instanceof Number) {
            return new Date(((Number) val).longValue());
        }
        return null;
    }

    public static <T> T field(Object obj, String name, JsonMapper mapper, Class<T> type) {
        return fields(obj).withKey(name).as(mapper, type);
    }

    public static <T> T field(Object obj, String name, Class<T> type) {
        return fields(obj).withKey(name).as(type);
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

    @SuppressWarnings("rawtypes")
    public <T> T asCollection(JsonMapper mapper, Class<? extends Collection> collectionClass, Class<?> elementsClass) {
        return mapper.convertCollectionValue(get(), collectionClass, elementsClass);
    }

    public <T> List<? extends T> asList(JsonMapper mapper, Class<T> elementsClass) {
        return asCollection(mapper, List.class, elementsClass);
    }

    @SuppressWarnings("unchecked")
    public <T> T as(Class<T> clz) {
        Object obj = get();

        // Doing .as(Boolean.class) will convert null to false, so do it manually
        if (clz == Boolean.class && obj == null) {
            return null;
        }
        return (T) ConvertUtils.convert(obj, clz);
    }

    public Object get() {
        Map<String, Object> map = getTargetMap(false, true);
        Object result = key == null ? null : map.get(key);
        return result == null ? defaultValue : result;
    }

    public Object getForWrite() {
        Map<String, Object> map = getTargetMap(false, false);
        Object result = key == null ? null : map.get(key);
        return result == null ? defaultValue : result;
    }

    public void set(Object value) {
        Map<String, Object> map = getTargetMap(true, false);
        if (key != null) {
            map.put(key, value);
        }
    }

    public void remove() {
        Map<String, Object> map = getTargetMap(true, false);
        if (key != null) {
            map.remove(key);
        }
    }

    protected Map<String, Object> getTargetMap(boolean addContainer, boolean read) {
        Map<String, Object> sourceMap = this.sourceMap;

        if (sourceMap == null && source != null) {
            sourceMap = getData(source, read);
        }

        if (sourceMap == null) {
            if (!addContainer) {
                throw new IllegalStateException("Can not set a value on a null target");
            }
            return null;
        }

        Map<String, Object> map = sourceMap;

        if (isScopeSet()) {
            Object scopedMap = sourceMap.get(getScope());
            if (scopedMap == null && addContainer) {
                scopedMap = new HashMap<String, Object>();
                sourceMap.put(getScope(), scopedMap);
            }
            map = CollectionUtils.toMap(scopedMap);
        }

        return map;
    }

    protected static Map<String, Object> getData(Object obj, boolean read) {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) ObjectUtils.getPropertyIgnoreErrors(obj, DataUtils.DATA);

        if (read) {
            return map == null ? Collections.<String, Object> emptyMap() : Collections.unmodifiableMap(map);
        } else if (map instanceof UnmodifiableMap<?, ?>) {
            map = ((UnmodifiableMap<String, Object>) map).getModifiableCopy();
        } else if (map == null) {
            map = new HashMap<>();
        }

        ObjectUtils.setProperty(obj, DataUtils.DATA, map);
        return map;
    }

    protected boolean isScopeSet() {
        return scope != null || scopeKey != null;
    }

    protected String getScope() {
        return scope == null ? scopeKey : scope.getName();
    }
}
