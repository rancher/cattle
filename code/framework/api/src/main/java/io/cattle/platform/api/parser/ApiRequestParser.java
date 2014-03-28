package io.cattle.platform.api.parser;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.gdapi.request.parser.DefaultApiRequestParser;

import com.netflix.config.DynamicBooleanProperty;

public class ApiRequestParser extends DefaultApiRequestParser {

    private static final DynamicBooleanProperty ALLOW_OVERRIDE = ArchaiusUtil.getBoolean("api.allow.client.override");

    @Override
    public boolean isAllowClientOverrideHeaders() {
        return ALLOW_OVERRIDE.get();
    }

}
