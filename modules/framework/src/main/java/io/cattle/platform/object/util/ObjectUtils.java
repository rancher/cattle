package io.cattle.platform.object.util;

import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.framework.event.FrameworkEvents;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.util.type.CollectionUtils;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ObjectUtils {

    public static void copy(Object src, Object dest) {
        try {
            BeanUtils.copyProperties(dest, src);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Failed to copy properties from [" + src + "] to [" + dest + "]", e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException("Failed to copy properties from [" + src + "] to [" + dest + "]", e);
        }
    }

    public static Object getAccountId(Object obj) {
        return getPropertyIgnoreErrors(obj, ObjectMetaDataManager.ACCOUNT_FIELD);
    }

    public static boolean isSystem(Object obj) {
        return Boolean.TRUE.equals(getPropertyIgnoreErrors(obj, ObjectMetaDataManager.SYSTEM_FIELD));
    }

    public static Object getId(Object obj) {
        return getPropertyIgnoreErrors(obj, ObjectMetaDataManager.ID_FIELD);
    }

    public static String getState(Object obj) {
        Object result = getPropertyIgnoreErrors(obj, ObjectMetaDataManager.STATE_FIELD);
        return result == null ? null : result.toString();
    }

    public static Date getRemoved(Object obj) {
        Object result = getPropertyIgnoreErrors(obj, ObjectMetaDataManager.REMOVED_FIELD);
        return result instanceof Date ? (Date) result : null;
    }

    public static Date getRemoveTime(Object obj) {
        Object result = getPropertyIgnoreErrors(obj, ObjectMetaDataManager.REMOVE_TIME_FIELD);
        return result instanceof Date ? (Date) result : null;
    }

    public static String getKind(Object obj) {
        Object kind = getPropertyIgnoreErrors(obj, ObjectMetaDataManager.KIND_FIELD);
        return kind == null ? null : kind.toString();
    }

    public static boolean hasWritableProperty(Object obj, String name) {
        if (obj == null) {
            return false;
        }

        try {
            PropertyDescriptor desc = PropertyUtils.getPropertyDescriptor(obj, name);
            return desc != null && desc.getWriteMethod() != null;
        } catch (IllegalAccessException e) {
            return false;
        } catch (InvocationTargetException e) {
            return false;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    public static Object getPropertyIgnoreErrors(Object obj, String property) {
        try {
            if (obj == null) {
                return null;
            }
            return PropertyUtils.getProperty(obj, property);
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        } catch (NoSuchMethodException e) {
        }

        return null;
    }

    public static Object getProperty(Object obj, String property) {
        try {
            if (obj == null) {
                return null;
            }
            return PropertyUtils.getProperty(obj, property);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Failed to get property [" + property + "] on [" + obj + "]", e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException("Failed to get property [" + property + "] on [" + obj + "]", e);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Failed to get property [" + property + "] on [" + obj + "]", e);
        }
    }

    public static void setProperty(Object obj, String property, Object value) {
        try {
            if (obj == null) {
                return;
            }
            PropertyUtils.setProperty(obj, property, value);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Failed to set property [" + property + "] on [" + obj + "] with value [" + value + "]", e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException("Failed to set property [" + property + "] on [" + obj + "] with value [" + value + "]", e);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Failed to set property [" + property + "] on [" + obj + "] with value [" + value + "]", e);
        }
    }

    public static void setPropertyIgnoreErrors(Object obj, String property, Object value) {
        try {
            if (obj == null) {
                return;
            }
            PropertyUtils.setProperty(obj, property, value);
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        } catch (NoSuchMethodException e) {
        }
    }

    public static ObjectToStringWrapper toStringWrapper(Object obj) {
        return new ObjectToStringWrapper(obj);
    }

    public static Object getValue(Object obj, String name) {
        Object val = getPropertyIgnoreErrors(obj, name);
        if (val == null) {
            val = DataAccessor.field(obj, name, Object.class);
        }
        return val;
    }

    private static final class ObjectToStringWrapper {
        Object obj;

        public ObjectToStringWrapper(Object obj) {
            super();
            this.obj = obj;
        }

        @Override
        public String toString() {
            if (obj == null) {
                return "null";
            }
            return String.format("%s:%s", obj.getClass().getSimpleName(), ObjectUtils.getId(obj));
        }
    }

    public static String toString(Object obj) {
        return Objects.toString(obj, null);
    }

    public static void publishChanged(EventService eventService, ObjectManager objectManager, Object obj) {
        if (obj == null) {
            return;
        }

        Object accountId = getAccountId(obj);
        Object resourceId = getId(obj);
        String type = objectManager.getType(obj);
        publishChanged(eventService, accountId, resourceId, type);
    }

    public static void publishChanged(final EventService eventService, Object accountId, Object resourceId, String resourceType) {
        Map<String, Object> data = new HashMap<>();
        data.put(ObjectMetaDataManager.ACCOUNT_FIELD, accountId);

        final Event event = EventVO.newEvent(FrameworkEvents.STATE_CHANGE)
                .withData(data)
                .withResourceType(resourceType)
                .withResourceId(toString(resourceId));

        eventService.publish(event);
    }

    public static boolean areObjectsEqual(Object a, Object b) {
        // covers both being nulls and primitives
        if (a == b) {
            return true;
        }

        // covers both being not null
        if ((a != null) != (b != null)) {
            if (a instanceof String) {
                if (b == null) {
                    b = "";
                }
            } else if (b instanceof String) {
                if (a == null) {
                    a = "";
                }
            } else {
                return false;
            }
        }

        if (a instanceof String || a instanceof Long || a instanceof Integer || a instanceof Boolean) {
            return StringUtils.equalsIgnoreCase(a.toString(), b.toString());
        } else if (a instanceof List) {
            return compareLists(a, b);
        } else if (a instanceof Map) {
            return compareMaps(CollectionUtils.toMap(a), CollectionUtils.toMap(b));
        } else {
            areObjectsEqual(CollectionUtils.toMap(a), CollectionUtils.toMap(b));
        }

        return false;
    }


    @SuppressWarnings("unchecked")
    private static boolean compareLists(Object a, Object b) {
        for (Object aItem : (List<Object>) a) {
            boolean found = false;
            for (Object bItem : (List<Object>) b) {
                if (areObjectsEqual(aItem, bItem)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }

        for (Object bItem : (List<Object>) b) {
            boolean found = false;
            for (Object aItem : (List<Object>) a) {
                if (areObjectsEqual(aItem, bItem)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private static boolean compareMaps(Map<Object, Object> a, Map<Object, Object> b) {
        if (!compareMap(a, b)) {
            return false;
        }
        return compareMap(b, a);
    }

    private static boolean compareMap(Map<Object, Object> aMap, Map<Object, Object> bMap) {
        for (Object aKey : aMap.keySet()) {
            if (!bMap.containsKey(aKey)) {
                return false;
            }

            if (!areObjectsEqual(aMap.get(aKey), bMap.get(aKey))) {
                return false;
            }
        }
        return true;
    }

}