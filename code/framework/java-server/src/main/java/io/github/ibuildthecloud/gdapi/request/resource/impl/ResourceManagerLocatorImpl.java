package io.github.ibuildthecloud.gdapi.request.resource.impl;

import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManagerFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManagerLocator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

public class ResourceManagerLocatorImpl extends AbstractResourceManagerLocatorImpl implements ResourceManagerLocator {

    List<ResourceManager> resourceManagers;
    List<ResourceManagerFilter> resourceManagerFilters;
    ResourceManager defaultResourceManager;
    Map<String, List<ResourceManagerFilter>> resourceManagerFiltersByType = new LinkedHashMap<String, List<ResourceManagerFilter>>();
    Map<String, ResourceManager> resourceManagersByType = new LinkedHashMap<String, ResourceManager>();

    /* Should not be used at runtime, just for startup */
    SchemaFactory schemaFactory;

    @PostConstruct
    public void init() {
        resourceManagersByType.clear();
        resourceManagerFiltersByType.clear();

        for (ResourceManager rm : resourceManagers) {
            for (String type : rm.getTypes()) {
                resourceManagersByType.put(type, rm);
            }
            for (Class<?> clz : rm.getTypeClasses()) {
                String type = schemaFactory.getSchemaName(clz);
                if (type != null)
                    resourceManagersByType.put(type, rm);
            }
        }

        for (ResourceManagerFilter filter : resourceManagerFilters) {
            Set<String> once = new HashSet<>();
            for (String type : filter.getTypes()) {
                if (once.contains(type)) {
                    continue;
                }
                once.add(type);
                add(resourceManagerFiltersByType, type, filter);
            }
            for (Class<?> clz : filter.getTypeClasses()) {
                String type = schemaFactory.getSchemaName(clz);
                if (type != null) {
                    if (once.contains(type)) {
                        continue;
                    }
                    once.add(type);
                    add(resourceManagerFiltersByType, type, filter);
                }
            }
        }
    }

    @Override
    protected void add(Map<String, List<ResourceManagerFilter>> filters, String key, ResourceManagerFilter filter) {
        List<ResourceManagerFilter> list = filters.get(key);
        if (list == null) {
            list = new ArrayList<ResourceManagerFilter>();
            filters.put(key, list);
        }

        list.add(filter);
    }

    @Override
    protected ResourceManager getResourceManagersByTypeInternal(String type) {
        return resourceManagersByType.get(type);
    }

    @Override
    protected List<ResourceManagerFilter> getResourceManagerFiltersByTypeInternal(String type) {
        return resourceManagerFiltersByType.get(type);
    }

    @Override
    public ResourceManager getDefaultResourceManager() {
        return defaultResourceManager;
    }

    public void setDefaultResourceManager(ResourceManager defaultResourceManager) {
        this.defaultResourceManager = defaultResourceManager;
    }

    public List<ResourceManagerFilter> getResourceManagerFilters() {
        return resourceManagerFilters;
    }

    @Inject
    public void setResourceManagerFilters(List<ResourceManagerFilter> resourceManagerFilters) {
        this.resourceManagerFilters = resourceManagerFilters;
    }

    public List<ResourceManager> getResourceManagers() {
        return resourceManagers;
    }

    @Inject
    public void setResourceManagers(List<ResourceManager> resourceManagers) {
        this.resourceManagers = resourceManagers;
    }

    public SchemaFactory getSchemaFactory() {
        return schemaFactory;
    }

    public void setSchemaFactory(SchemaFactory schemaFactory) {
        this.schemaFactory = schemaFactory;
    }

}