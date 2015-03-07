package io.cattle.platform.configitem.server.resource;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbstractResource implements Resource {

    Map<String, Object> attributes = new LinkedHashMap<String, Object>();
    String name;

    public AbstractResource(String name) {
        this.name = name;
    }

    @Override
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Object getAttibute(String attribute) {
        return attributes.get(attribute);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Resource [" + name + "]";
    }

}
