package io.cattle.platform.object.postinit;

import java.util.Map;

public interface ObjectPostInstantiationHandler {

    public <T> T postProcess(T obj, Class<T> clz, Map<String, Object> properties);

}
