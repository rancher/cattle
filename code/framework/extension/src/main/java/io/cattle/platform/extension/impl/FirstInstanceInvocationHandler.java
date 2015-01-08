package io.cattle.platform.extension.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class FirstInstanceInvocationHandler implements InvocationHandler {

    Object obj = new Object();
    ExtensionList<?> list;

    public FirstInstanceInvocationHandler(ExtensionList<?> list) {
        super();
        this.list = list;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(obj, args);
        }

        Object first = list.size() > 0 ? list.get(0) : null;
        return first == null ? first : method.invoke(first, args);
    }

}
