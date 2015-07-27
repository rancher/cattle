package io.github.ibuildthecloud.gdapi.response.impl;

import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;

public class ResourceOutputFilterChain implements ResourceOutputFilter {

    ResourceOutputFilter current;
    ResourceOutputFilter next;

    public ResourceOutputFilterChain(ResourceOutputFilter current, ResourceOutputFilter next) {
        super();
        this.current = current;
        this.next = next;
    }

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        Resource result = current.filter(request, original, converted);

        if (result != null) {
            result = next.filter(request, original, result);
        }

        return result;
    }

    @Override
    public String[] getTypes() {
        return new String[0];
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[0];
    }

}
