package io.github.ibuildthecloud.gdapi.response;

import io.github.ibuildthecloud.gdapi.request.ApiRequest;

public interface HtmlTemplate {
    byte[] getHeader(ApiRequest request, Object response);

    byte[] getFooter(ApiRequest request, Object response);
}
