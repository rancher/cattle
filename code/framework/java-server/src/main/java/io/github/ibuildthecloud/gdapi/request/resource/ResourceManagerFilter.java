package io.github.ibuildthecloud.gdapi.request.resource;

import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.List;
import java.util.Map;

public interface ResourceManagerFilter {

    String[] getTypes();

    Class<?>[] getTypeClasses();

    Object getById(String type, String id, ListOptions options, ResourceManager next);

    Object getLink(String type, String id, String link, ApiRequest request, ResourceManager next);

    Object list(String type, ApiRequest request, ResourceManager next);

    List<?> list(String type, Map<Object, Object> criteria, ListOptions options, ResourceManager next);

    Object create(String type, ApiRequest request, ResourceManager next);

    Object update(String type, String id, ApiRequest request, ResourceManager next);

    Object delete(String type, String id, ApiRequest request, ResourceManager next);

    Object resourceAction(String type, ApiRequest request, ResourceManager next);

    Object collectionAction(String type, ApiRequest request, ResourceManager next);
}
