package io.github.ibuildthecloud.gdapi.response;

import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;
import java.io.OutputStream;

import javax.inject.Inject;

public class HtmlResponseWriter extends JsonResponseWriter {

    HtmlTemplate htmlTemplate;

    @Override
    protected String getContentType() {
        return "text/html; charset=utf-8";
    }

    @Override
    protected String getResponseFormat() {
        return "html";
    }

    @Override
    protected void writeJson(OutputStream os, Object responseObject, ApiRequest request) throws IOException {
        os.write(htmlTemplate.getHeader(request, responseObject));
        super.writeJson(os, responseObject, request);
        os.write(htmlTemplate.getFooter(request, responseObject));
    }

    public HtmlTemplate getHtmlTemplate() {
        return htmlTemplate;
    }

    @Inject
    public void setHtmlTemplate(HtmlTemplate htmlTemplate) {
        this.htmlTemplate = htmlTemplate;
    }

}
