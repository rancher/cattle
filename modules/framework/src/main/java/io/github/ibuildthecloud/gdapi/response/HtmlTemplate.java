package io.github.ibuildthecloud.gdapi.response;

import io.github.ibuildthecloud.gdapi.request.ApiRequest;

public interface HtmlTemplate {
    public byte[] getHeader(ApiRequest request, Object response);

    public byte[] getFooter(ApiRequest request, Object response);
}
