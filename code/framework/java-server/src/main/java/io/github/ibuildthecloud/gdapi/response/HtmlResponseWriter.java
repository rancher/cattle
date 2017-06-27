package io.github.ibuildthecloud.gdapi.response;

import io.github.ibuildthecloud.gdapi.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;
import java.io.OutputStream;

public class HtmlResponseWriter extends JsonResponseWriter {

    HtmlTemplate htmlTemplate;

    public HtmlResponseWriter(JsonMapper jsonMapper, HtmlTemplate htmlTemplate) {
        super(jsonMapper);
        this.htmlTemplate = htmlTemplate;
    }

    @Override
    protected String getContentType() {
        return "text/html; charset=utf-8";
    }

    @Override
    protected String getResponseFormat() {
        return "html";
    }

    @Override
    protected void writeJson(JsonMapper jsonMapper, OutputStream os, Object responseObject, ApiRequest request) throws IOException {
        os.write(htmlTemplate.getHeader(request, responseObject));
        super.writeJson(jsonMapper, os, responseObject, request);
        os.write(htmlTemplate.getFooter(request, responseObject));
    }

}
