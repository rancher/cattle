package io.github.ibuildthecloud.gdapi.response.impl;

import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilterManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

public class ResourceOutputFilterManagerImpl implements ResourceOutputFilterManager {

    SchemaFactory baseSchemaFactory;
    List<ResourceOutputFilter> outputFilters;
    Map<String, ResourceOutputFilter> filtersByType;

    @Override
    public ResourceOutputFilter getOutputFilter(Resource resource) {
        if (resource == null) {
            return null;
        }

        return getFiltersByType().get(resource.getType());
    }

    protected Map<String, ResourceOutputFilter> getFiltersByType() {
        if (filtersByType != null) {
            return filtersByType;
        }

        Map<String, ResourceOutputFilter> result = new HashMap<String, ResourceOutputFilter>();

        for (ResourceOutputFilter filter : outputFilters) {
            Set<String> types = new HashSet<String>();

            String[] typeStrings = filter.getTypes();
            for (String type : typeStrings) {
                types.add(type);
            }

            for (Class<?> clz : filter.getTypeClasses()) {
                if (typeStrings.length == 0) {
                    types.addAll(baseSchemaFactory.getSchemaNames(clz));
                } else {
                    types.add(baseSchemaFactory.getSchemaName(clz));
                }
            }

            for (String type : types) {
                ResourceOutputFilter next = result.get(type);

                if (next == null) {
                    result.put(type, filter);
                } else {
                    result.put(type, new ResourceOutputFilterChain(filter, next));
                }
            }
        }

        return filtersByType = result;
    }

    public List<ResourceOutputFilter> getOutputFilters() {
        return outputFilters;
    }

    @Inject
    public void setOutputFilters(List<ResourceOutputFilter> outputFilters) {
        this.outputFilters = outputFilters;
    }

    public SchemaFactory getBaseSchemaFactory() {
        return baseSchemaFactory;
    }

    public void setBaseSchemaFactory(SchemaFactory baseSchemaFactory) {
        this.baseSchemaFactory = baseSchemaFactory;
    }

}
