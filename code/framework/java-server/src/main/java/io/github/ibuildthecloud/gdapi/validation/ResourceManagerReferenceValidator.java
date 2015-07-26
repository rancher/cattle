package io.github.ibuildthecloud.gdapi.validation;

import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManagerLocator;
import io.github.ibuildthecloud.gdapi.util.RequestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class ResourceManagerReferenceValidator implements ReferenceValidator {

    ResourceManagerLocator locator;

    @Override
    public Object getById(String type, String id) {
        ResourceManager manager = locator.getResourceManagerByType(type);
        return manager == null ? null : manager.getById(type, id, null);
    }

    @Override
    public Object getByField(String type, String fieldName, Object value, String id) {
        ResourceManager manager = locator.getResourceManagerByType(type);

        if (manager == null) {
            return null;
        }

        Map<Object, Object> criteria = new HashMap<Object, Object>();
        criteria.put(fieldName, value);
        Object result = manager.list(type, criteria, null);
        List<?> list = RequestUtils.toList(result);
        if (StringUtils.isNotBlank(id)) {
            Object x = manager.getById(type, id, null);
            if (list.size() > 0 && x != null) {
                list.remove(x);
            }
        }
        return list.size() == 0 ? null : list.get(0);
    }

    @Override
    public Resource getResourceId(String type, String id) {
        ResourceManager manager = locator.getResourceManagerByType(type);
        if (manager == null) {
            return null;
        }

        Object result = manager.getById(type, id, null);
        if (result == null) {
            return null;
        }

        return manager.convertResponse(result, null);
    }

    public ResourceManagerLocator getLocator() {
        return locator;
    }

    @Inject
    public void setLocator(ResourceManagerLocator locator) {
        this.locator = locator;
    }

}
