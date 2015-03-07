package io.cattle.platform.iaas.api.request.handler;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.AbstractResponseGenerator;
import io.github.ibuildthecloud.gdapi.request.parser.DefaultApiRequestParser;

import java.io.IOException;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringProperty;

public class UIRedirect extends AbstractResponseGenerator {

    private static final DynamicBooleanProperty DO_REDIRECT = ArchaiusUtil.getBoolean("api.redirect.to.ui");
    private static final DynamicStringProperty UI_REDIRECT = ArchaiusUtil.getString("api.redirect.ui.path");

    @Override
    protected void generate(ApiRequest request) throws IOException {
        if (request.getRequestVersion() != null || !DO_REDIRECT.get() || !DefaultApiRequestParser.HTML.equals(request.getResponseFormat()))
            return;

        request.getServletContext().getResponse().sendRedirect(UI_REDIRECT.get());
        request.setResponseObject(new Object());
        request.commit();
    }

}
