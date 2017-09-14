package io.github.ibuildthecloud.gdapi.request.resource;

import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.List;
import java.util.Map;

public interface ResourceManager {

    Object getById(String type, String id, ListOptions options);

    Object list(String type, ApiRequest request);

    List<?> list(String type, Map<Object, Object> criteria, ListOptions options);

    Object create(String type, ApiRequest request);

    Object update(String type, String id, ApiRequest request);

    Object delete(String type, String id, ApiRequest request);

}
