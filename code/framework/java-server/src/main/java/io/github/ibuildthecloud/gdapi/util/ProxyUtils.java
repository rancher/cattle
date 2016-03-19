package io.github.ibuildthecloud.gdapi.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class ProxyUtils {

    @SuppressWarnings("unchecked")
    public static <T> T proxy(final Map<String, Object> map, Class<T> typeClz) {
        final Object obj = new Object();

        return (T)Proxy.newProxyInstance(typeClz.getClassLoader(), new Class<?>[] { typeClz }, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getDeclaringClass() == Object.class) {
                    return method.invoke(obj, args);
                }

                if (method.getName().startsWith("get")) {
                    String name = StringUtils.uncapitalize(method.getName().substring(3));
                    Object val = map.get(name);
                    if (val instanceof Long && (Integer.class.equals(method.getReturnType()) || Integer.TYPE.equals(method.getReturnType()))) {
                        return ((Long)val).intValue();
                    }
                    return val;
                }

                if (method.getName().startsWith("set") && args.length == 1) {
                    String name = StringUtils.uncapitalize(method.getName().substring(3));
                    map.put(name, args[0]);
                }

                return null;
            }
        });
    }
}
