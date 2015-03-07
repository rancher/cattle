package io.cattle.platform.api.link;

import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;

public interface LinkHandler {

    String[] getTypes();

    boolean handles(String type, String id, String link, ApiRequest request);

    Object link(String name, Object obj, ApiRequest request) throws IOException;

}
