package io.cattle.platform.extension.impl;

import io.cattle.platform.extension.ExtensionImplementation;
import io.cattle.platform.util.exception.ExceptionUtils;

import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;

public class ExtensionImplementationImpl implements ExtensionImplementation {

    String name, className;
    Map<String, String> properties = new HashMap<String, String>();

    public ExtensionImplementationImpl(String name, Object obj) {
        super();
        this.name = name;
        this.className = obj.getClass().getName();
        try {
            for (PropertyDescriptor desc : PropertyUtils.getPropertyDescriptors(obj)) {
                if (desc.getReadMethod() != null && (desc.getPropertyType().isPrimitive() || desc.getPropertyType().getName().startsWith("java."))
                        && !desc.getName().equals("class")) {
                    Object value = PropertyUtils.getProperty(obj, desc.getName());
                    properties.put(desc.getName(), value == null ? null : value.toString());
                }
            }
        } catch (Throwable t) {
            properties.put("error", ExceptionUtils.toString(t));
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

}
