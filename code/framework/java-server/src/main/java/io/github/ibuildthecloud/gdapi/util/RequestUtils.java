package io.github.ibuildthecloud.gdapi.util;

import static io.github.ibuildthecloud.gdapi.model.Schema.Method.*;

import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.ObjectUtils;

public class RequestUtils {

    public static boolean isBrowser(HttpServletRequest request, boolean checkAccepts) {
        String accepts = request.getHeader("Accept");
        String userAgent = request.getHeader("User-Agent");

        if (accepts == null || !checkAccepts) {
            accepts = "*/*";
        }

        accepts = accepts.toLowerCase();

        // User agent has Mozilla and browser accepts */*
        return (userAgent != null && userAgent.toLowerCase().indexOf("mozilla") != -1 && accepts.indexOf("*/*") != -1);
    }

    public static boolean isReadMethod(String method) {
        return !isWriteMethod(method);
    }

    public static boolean isWriteMethod(String method) {
        return POST.isMethod(method) || PUT.isMethod(method) || DELETE.isMethod(method);
    }

    public static boolean mayHaveBody(String method) {
        return POST.isMethod(method) || PUT.isMethod(method);
    }

    public static String getSingularStringValue(String key, Map<String, Object> params) {
        Object obj = params.get(key);
        Object singleObj = makeSingular(obj);
        return ObjectUtils.toString(singleObj, null);
    }
    
    public static String getConditionValue(String key, Map<Object, Object> params) {
        Object value = params.get(key);
        if (value == null) {
            return null;
        }
        
        value = makeSingular(value);
        if (value instanceof Condition) {
            return ObjectUtils.toString(((Condition) value).getValue(), null);
        }

        return ObjectUtils.toString(value, null);
    }

    public static Object makeSingular(Object input) {
        if (input instanceof List) {
            List<?> list = (List<?>)input;
            return list.size() == 0 ? null : list.get(0);
        }

        if (input instanceof String[]) {
            String[] array = (String[])input;
            return array.length == 0 ? null : array[0];
        }

        return input;
    }

    public static Object makeSingularIfCan(Object input) {
        if (input instanceof List) {
            List<?> list = (List<?>)input;
            if (list.size() == 1)
                return makeSingularIfCan(list.get(0));
            if (list.size() == 0)
                return null;
        }

        if (input instanceof String[]) {
            String[] array = (String[])input;
            if (array.length == 1) {
                return makeSingularIfCan(array[0]);
            }
            if (array.length == 0) {
                return null;
            }
            return Arrays.asList(array);
        }

        if (input instanceof Condition && ((Condition)input).getConditionType() == ConditionType.EQ) {
            return makeSingularIfCan(((Condition)input).getValue());
        }

        return input;
    }

    public static String makeSingularStringIfCan(Object input) {
        Object result = makeSingularIfCan(input);
        return result == null ? null : result.toString();
    }

    public static boolean hasBeenHandled(ApiRequest request) {
        if (request.isCommitted() || request.getResponseObject() != null) {
            return true;
        }

        return false;

    }

    public static List<?> toList(Object obj) {
        if (obj instanceof List) {
            return (List<?>)obj;
        } else if (obj == null) {
            return Collections.emptyList();
        } else {
            return Arrays.asList(obj);
        }
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> toMap(Object obj) {
        if (obj == null) {
            return new HashMap<K, V>();
        }

        if (obj instanceof Map) {
            return (Map<K, V>)obj;
        } else {
            return new HashMap<K, V>();
        }
    }

}