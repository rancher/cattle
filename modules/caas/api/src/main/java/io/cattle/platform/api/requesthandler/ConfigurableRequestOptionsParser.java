package io.cattle.platform.api.requesthandler;

import com.netflix.config.DynamicStringListProperty;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.gdapi.request.handler.RequestOptionsParser;

import java.util.List;

public class ConfigurableRequestOptionsParser extends RequestOptionsParser {

    private static final DynamicStringListProperty OPTIONS = ArchaiusUtil.getList("api.request.options");

    @Override
    public List<String> getOptions() {
        return OPTIONS.get();
    }

}
