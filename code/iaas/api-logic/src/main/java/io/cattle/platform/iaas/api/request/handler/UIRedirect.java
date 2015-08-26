package io.cattle.platform.iaas.api.request.handler;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.AbstractResponseGenerator;
import io.github.ibuildthecloud.gdapi.request.parser.DefaultApiRequestParser;

import java.io.IOException;
import java.net.URL;

import com.netflix.config.DynamicBooleanProperty;

public class UIRedirect extends AbstractResponseGenerator {

    private static final DynamicBooleanProperty DO_REDIRECT = ArchaiusUtil.getBoolean("api.redirect.to.ui");

    @Override
    protected void generate(ApiRequest request) throws IOException {
        if (request.getRequestVersion() != null || !DO_REDIRECT.get() || !DefaultApiRequestParser.HTML.equals(request.getResponseFormat()))
            return;

        URL url = request.getUrlBuilder().staticResource();
        request.getServletContext().getResponse().sendRedirect(url.toExternalForm());
        request.setResponseObject(new Object());
        request.commit();
    }
}
