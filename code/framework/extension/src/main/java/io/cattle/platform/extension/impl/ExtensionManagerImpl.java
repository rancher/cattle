package io.cattle.platform.extension.impl;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.extension.ExtensionImplementation;
import io.cattle.platform.extension.ExtensionManager;
import io.cattle.platform.extension.ExtensionPoint;
import io.cattle.platform.util.type.CollectionUtils;
import io.cattle.platform.util.type.InitializationTask;
import io.cattle.platform.util.type.NamedUtils;
import io.cattle.platform.util.type.PriorityUtils;
import io.cattle.platform.util.type.ScopeUtils;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class ExtensionManagerImpl implements ExtensionManager, InitializationTask {

    private static final String WILDCARD = "regexp:";

    Map<String, List<Object>> byKeyRegistry = new HashMap<String, List<Object>>();
    Map<String, ExtensionList<Object>> extensionLists = new HashMap<String, ExtensionList<Object>>();
    Map<String, ExtensionMap<String, Object>> extensionMaps = new HashMap<>();
    Map<String, List<Object>> byName = new HashMap<String, List<Object>>();
    Map<Object, String> objectToName = new HashMap<Object, String>();
    Map<String, Class<?>> keyToType = new HashMap<String, Class<?>>();
    Map<Pattern, List<String>> wildcards = new HashMap<Pattern, List<String>>();
    boolean started = false;


    @SuppressWarnings("unchecked")
    @Override
    public <T> T first(String key, String typeString) {
        try {
            Class<?> clz = Class.forName(typeString);
            return first(key, (Class<T>) clz);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Failed to find class [" + typeString + "]", e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T first(String key, Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, new FirstInstanceInvocationHandler(getExtensionListInternal(key)));
    }

    @Override
    public List<?> list(String key) {
        return getExtensionListInternal(key);
    }

    @Override
    public Map<String, Object> map(String key) {
        return getExtensionMapInternal(key);
    }

    protected synchronized ExtensionMap<String, Object> getExtensionMapInternal(String key) {
        ExtensionMap<String, Object> map = extensionMaps.get(key);
        if (map == null) {
            Map<String, Object> inner = started ? getMap(key) : Collections.<String, Object>emptyMap();
            map = new ExtensionMap<>(this, key, inner);
            extensionMaps.put(key, map);
        }
        return map;
    }

    @Override
    public <T> List<T> getExtensionList(Class<T> type) {
        return getExtensionList(ScopeUtils.getScopeFromClass(type), type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> List<T> getExtensionList(String key, Class<T> type) {
        Class<?> clz = keyToType.get(key);
        if (type != null && clz != null && clz != type) {
            throw new IllegalArgumentException("Extension list for key [" + key + "] is of type [" + type + "] got [" + clz + "]");
        }
        return (List<T>) getExtensionListInternal(key);
    }

    protected synchronized ExtensionList<?> getExtensionListInternal(String key) {
        ExtensionList<Object> list = extensionLists.get(key);
        if (list == null) {
            List<Object> inner = started ? getList(key) : Collections.emptyList();
            list = new ExtensionList<Object>(this, key, inner);
            extensionLists.put(key, list);
        }

        return list;
    }

    public synchronized void addObject(String key, Class<?> clz, Object obj, String name) {
        Class<?> existing = keyToType.get(key);
        if (existing == null) {
            keyToType.put(key, clz);
        } else if (existing != clz) {
            throw new IllegalArgumentException("Can not change type of key [" + key + "] to [" + clz + "] already [" + existing + "]");
        }

        List<Object> objects = byKeyRegistry.get(key);
        if (objects == null) {
            objects = new CopyOnWriteArrayList<Object>();
            byKeyRegistry.put(key, objects);
        }

        objects.add(obj);

        CollectionUtils.addToMap(byName, name, obj, ArrayList.class);
        objectToName.put(obj, name);

        Pattern pattern = null;
        if (key.startsWith(WILDCARD)) {
            pattern = Pattern.compile(key.substring(0, WILDCARD.length()));
        } else if (key.contains("*")) {
            String[] parts = StringUtils.splitByWholeSeparator(key.trim(), "*");
            for (int i = 0; i < parts.length; i++) {
                parts[i] = Pattern.quote(parts[i].trim());
            }

            pattern = Pattern.compile(parts.length == 0 ? ".*" : StringUtils.join(parts, ".*"));
        }

        if (pattern != null) {
            CollectionUtils.addToMap(wildcards, pattern, name, ArrayList.class);
        }
    }

    @Override
    public synchronized void start() {
        if (!started) {
            for (Map.Entry<String, List<Object>> entry : byKeyRegistry.entrySet()) {
                String key = entry.getKey();
                ExtensionList<?> extensionList = getExtensionListInternal(key);
                extensionList.inner.clear();
                extensionList.inner.addAll(getList(key));
                ExtensionMap<String, Object> extensionMap = getExtensionMapInternal(key);
                extensionMap.inner.clear();
                extensionMap.inner.putAll(getMap(key));
            }

            started = true;
        }
    }

    protected synchronized Map<String, Object> getMap(String key) {
        List<?> list = getList(key);
        Map<String, Object> map = new ConcurrentHashMap<>();
        for (Object item : list) {
            map.put(NamedUtils.getName(item), item);
        }
        return map;
    }

    public void reset() {
        started = false;
        start();
    }

    protected synchronized List<Object> getList(String key) {
        Class<?> typeClz = keyToType.get(key);
        if (typeClz == null) {
            typeClz = Object.class;
        }

        Set<String> excludes = getSetting(key + ".exclude");

        String list = ArchaiusUtil.getString(key + ".list").get();
        if (!StringUtils.isBlank(list)) {
            List<Object> result = new ArrayList<Object>();
            for (String name : list.split("\\s*,\\s*")) {
                if (excludes.contains(name)) {
                    continue;
                }

                result.addAll(getObjectsByName(name, typeClz));
            }
            return result;
        }

        Set<String> includes = getSetting(key + ".include");

        Set<Object> ordered = new TreeSet<Object>(new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                int left = PriorityUtils.getPriority(o1);
                int right = PriorityUtils.getPriority(o2);
                if (left < right) {
                    return -1;
                } else if (left > right) {
                    return 1;
                }
                String leftName = objectToName.get(o1);
                String rightName = objectToName.get(o2);
                int comparisonResult = leftName.compareTo(rightName);
                if (comparisonResult == 0 && !o1.equals(o2)) {
                    throw new RuntimeException("Trying to add 2 objects with the same name: " + leftName + ".  Second object is ignored!");
                }
                return comparisonResult;
            }
        });

        List<?> registered = byKeyRegistry.get(key);

        if (registered != null) {
            ordered.addAll(registered);
        }

        ordered.addAll(getByWildcard(key, typeClz));

        for (String include : includes) {
            ordered.addAll(getObjectsByName(include, typeClz));
        }

        Iterator<Object> iter = ordered.iterator();
        while (iter.hasNext()) {
            String name = objectToName.get(iter.next());
            if (excludes.contains(name)) {
                iter.remove();
            }
        }

        return new ArrayList<Object>(ordered);
    }

    protected List<Object> getByWildcard(String key, Class<?> typeClz) {
        List<Object> result = new ArrayList<Object>();

        for (Map.Entry<Pattern, List<String>> entry : wildcards.entrySet()) {
            if (entry.getKey().matcher(key).matches()) {
                for (String name : entry.getValue()) {
                    result.addAll(getObjectsByName(name, typeClz));
                }
            }
        }

        return result;
    }

    protected List<Object> getObjectsByName(String name, Class<?> typeClz) {
        List<Object> result = new ArrayList<Object>();
        List<Object> objs = byName.get(name);
        if (objs != null) {
            for (Object obj : objs) {
                if (typeClz.isAssignableFrom(obj.getClass())) {
                    result.add(obj);
                }
            }
        }

        return result;
    }

    protected Set<String> getSetting(String key) {
        String value = getSettingValue(key);
        if (StringUtils.isBlank(value)) {
            return Collections.emptySet();
        }

        Set<String> result = new HashSet<String>();

        for (String part : value.trim().split("\\s*,\\s*")) {
            result.add(part);
        }

        return result;
    }

    protected String getSettingValue(String key) {
        return ArchaiusUtil.getString(key).get();
    }

    @Override
    public List<ExtensionPoint> getExtensions() {
        List<ExtensionPoint> result = new ArrayList<ExtensionPoint>();

        Set<String> keys = new TreeSet<String>(extensionLists.keySet());

        for (String key : keys) {
            result.add(getExtensionPoint(key));
        }

        if (keys.size() != extensionLists.size()) {
            /*
             * While traversing the extensions, more extension might be
             * registered so try again
             */
            return getExtensions();
        }

        return result;
    }

    @Override
    public ExtensionPoint getExtensionPoint(Class<?> type) {
        return getExtensionPoint(ScopeUtils.getScopeFromClass(type), type);
    }

    @Override
    public ExtensionPoint getExtensionPoint(String key, Class<?> type) {
        Class<?> clz = keyToType.get(key);
        if (clz != null && clz != type) {
            throw new IllegalArgumentException("Extension list for key [" + key + "] is of type [" + type + "] got [" + clz + "]");
        }
        return getExtensionPoint(key);
    }

    protected ExtensionPoint getExtensionPoint(String key) {
        List<ExtensionImplementation> impls = new ArrayList<ExtensionImplementation>();
        List<?> list = getExtensionList(key, null);

        if (list != null) {
            for (Object obj : list) {
                String name = objectToName.get(obj);
                if (name == null) {
                    name = "Dynamic : " + NamedUtils.getName(obj);
                }
                impls.add(new ExtensionImplementationImpl(name, obj));
            }
        }

        return new ExtensionPointImpl(key, impls, getSettingValue(key + ".list"), getSettingValue(key + ".exclude"), getSettingValue(key + ".include"));
    }

    protected Class<?> getExpectedType(String key) {
        return keyToType.get(key);
    }
}
