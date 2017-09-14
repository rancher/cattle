package io.cattle.platform.api.proxy;

import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class ProxyRequest {

    @SuppressWarnings("unchecked")
    public static <T> T proxy(ApiRequest request, Class<T> typeClz) {
        final Object obj = new Object();
        final Map<String, Object> map = CollectionUtils.toMap(request.getRequestObject());

        return (T) Proxy.newProxyInstance(typeClz.getClassLoader(), new Class<?>[] { typeClz }, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getDeclaringClass() == Object.class) {
                    return method.invoke(obj, args);
                }

                if (method.getName().startsWith("get")) {
                    String name = StringUtils.uncapitalize(method.getName().substring(3));
                    return map.get(name);
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
