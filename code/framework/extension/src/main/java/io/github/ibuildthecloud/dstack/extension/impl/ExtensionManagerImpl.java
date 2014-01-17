package io.github.ibuildthecloud.dstack.extension.impl;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.extension.ExtensionImplementation;
import io.github.ibuildthecloud.dstack.extension.ExtensionManager;
import io.github.ibuildthecloud.dstack.extension.ExtensionPoint;
import io.github.ibuildthecloud.dstack.util.type.InitializationTask;
import io.github.ibuildthecloud.dstack.util.type.NamedUtils;
import io.github.ibuildthecloud.dstack.util.type.PriorityUtils;
import io.github.ibuildthecloud.dstack.util.type.ScopeUtils;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtensionManagerImpl implements ExtensionManager, InitializationTask {

    private static final Logger log = LoggerFactory.getLogger(ExtensionManagerImpl.class);

    Map<String,List<Object>> byKeyRegistry = new HashMap<String, List<Object>>();
    Map<String,ExtensionList<Object>> extensionLists = new HashMap<String, ExtensionList<Object>>();
    Map<String,Object> byName = new HashMap<String, Object>();
    Map<Object,String> objectToName = new HashMap<Object,String>();
    Map<String,Class<?>> keyToType = new HashMap<String, Class<?>>();
//    Map<String,Set<Runnable>> callbacks = Collections.synchronizedMap(new HashMap<String, Set<Runnable>>());
    boolean started = false;


    @SuppressWarnings("unchecked")
    @Override
    public <T> T first(String key, String typeString) {
        try {
            Class<?> clz = Class.forName(typeString);
            return first(key, (Class<T>)clz);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Failed to find class [" + typeString + "]", e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T first(String key, Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
                new FirstInstanceInvocationHandler(getExtensionListInternal(key)));
    }

    @Override
    public List<?> list(String key) {
        return getExtensionListInternal(key);
    }

    @Override
    public <T> List<T> getExtensionList(Class<T> type) {
        return getExtensionList(ScopeUtils.getScopeFromClass(type), type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> List<T> getExtensionList(String key, Class<T> type) {
        Class<?> clz = keyToType.get(key);
        if ( clz != null && clz != type ) {
            throw new IllegalArgumentException("Extension list for key [" + key + "] is of type ["
                    + type + "] got [" + clz + "]");
        }
        return (List<T>)getExtensionListInternal(key);
    }

    protected synchronized ExtensionList<?> getExtensionListInternal(String key) {
        ExtensionList<Object> list = extensionLists.get(key);
        if ( list == null ) {
            list = new ExtensionList<Object>(this, key, Collections.emptyList());
            extensionLists.put(key, list);
        }

        return list;
    }

    public synchronized void addObject(String key, Class<?> clz, Object obj) {
        addObject(key, clz, obj, NamedUtils.getName(obj));
    }

    public synchronized void addObject(String key, Class<?> clz, Object obj, String name) {
        Class<?> existing = keyToType.get(key);
        if ( existing == null ) {
            keyToType.put(key, clz);
        } else if ( existing != clz ) {
            throw new IllegalArgumentException("Can not change type of key [" + key
                    + "] to [" + clz + "] already [" + existing + "]");
        }

        List<Object> objects = byKeyRegistry.get(key);
        if ( objects == null ) {
            objects = new CopyOnWriteArrayList<Object>();
            byKeyRegistry.put(key, objects);
        }

        objects.add(obj);

        if ( byName.get(name) != null ) {
            log.info("Extension of name [{}] already exists [{}], overriding with [{}]", name, byName.get(name), obj);
        }

        byName.put(name, obj);
        objectToName.put(obj, name);
    }

    @Override
    public synchronized void start() {
        if ( ! started ) {
            for ( Map.Entry<String, List<Object>> entry : byKeyRegistry.entrySet() ) {
                String key = entry.getKey();
                ExtensionList<?> extensionList = getExtensionListInternal(key);
                extensionList.inner.clear();
                extensionList.inner.addAll(getList(entry.getValue(), key));
            }

            started = true;
        }
    }

    public void reset() {
        started = false;
        start();
    }

    @Override
    public synchronized void stop() {
    }

    protected List<?> getList(List<Object> existing, String key) {
        Class<?> typeClz = keyToType.get(key);
        if ( typeClz == null ) {
            typeClz = Object.class;
        }

        Set<String> excludes = getSetting(key + ".exclude");

        String list = ArchaiusUtil.getString(key + ".list").get();
        if ( ! StringUtils.isBlank(list) ) {
            List<Object> result = new ArrayList<Object>();
            for ( String name : list.split("\\s*,\\s*") ) {
                Object obj = byName.get(name);
                if ( ! excludes.contains(name) && obj != null && typeClz.isAssignableFrom(obj.getClass()) ) {
                    result.add(obj);
                }
            }
            return result;
        }


        Set<String> includes = getSetting(key + ".include");

        Set<Object> ordered = new TreeSet<Object>(new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                int left = PriorityUtils.getPriority(o1);
                int right = PriorityUtils.getPriority(o2);
                if ( left < right ) {
                    return -1;
                } else if ( left > right ) {
                    return 1;
                }
                String leftName = objectToName.get(o1);
                String rightName = objectToName.get(o2);
                return leftName.compareTo(rightName);
            }
        });

        List<?> registered = byKeyRegistry.get(key);

        if ( registered != null ) {
            for ( Object obj : registered ) {
                String name = objectToName.get(obj);
                if ( ! excludes.contains(name) ) {
                    ordered.add(obj);
                }
            }
        }

        for ( String include : includes ) {
            Object obj = byName.get(include);
            if ( obj != null && typeClz.isAssignableFrom(obj.getClass()) ) {
                ordered.add(obj);
            }
        }

        return new ArrayList<Object>(ordered);
    }

    protected Set<String> getSetting(String key) {
        String value = getSettingValue(key);
        if ( StringUtils.isBlank(value) ) {
            return Collections.emptySet();
        }

        Set<String> result = new HashSet<String>();

        for ( String part : value.trim().split("\\s*,\\s*") ) {
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

        for ( String key : keys ) {
            result.add(getExtensionPoint(key));
        }

        if ( keys.size() != extensionLists.size() ) {
            /* While traversing the extensions, more extension might be registered
             * so try again
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
        if ( clz != null && clz != type ) {
            throw new IllegalArgumentException("Extension list for key [" + key + "] is of type ["
                    + type + "] got [" + clz + "]");
        }
        return getExtensionPoint(key);
    }

    protected ExtensionPoint getExtensionPoint(String key) {
        List<ExtensionImplementation> impls = new ArrayList<ExtensionImplementation>();
        ExtensionList<Object> list = extensionLists.get(key);

        if ( list != null ) {
            for ( Object obj : list ) {
                String name = objectToName.get(obj);
                impls.add(new ExtensionImplementationImpl(name, obj));
            }
        }

        return new ExtensionPointImpl(key, impls, getSettingValue(key + ".list"),
                getSettingValue(key + ".exclude"), getSettingValue(key + ".include"));
    }
}