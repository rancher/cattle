package io.github.ibuildthecloud.gdapi.request.resource;

import io.github.ibuildthecloud.gdapi.model.Collection;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.List;
import java.util.Map;

public interface ResourceManager {

    String[] getTypes();

    Class<?>[] getTypeClasses();

    Object getById(String type, String id, ListOptions options);

    Object getLink(String type, String id, String link, ApiRequest request);

    Object list(String type, ApiRequest request);

    List<?> list(String type, Map<Object, Object> criteria, ListOptions options);

    Object create(String type, ApiRequest request);

    Object update(String type, String id, ApiRequest request);

    Object delete(String type, String id, ApiRequest request);

    Object resourceAction(String type, ApiRequest request);

    Object collectionAction(String type, ApiRequest request);

    Collection convertResponse(List<?> object, ApiRequest request);

    Resource convertResponse(Object obj, ApiRequest request);

    boolean handleException(Throwable t, ApiRequest request);
}
