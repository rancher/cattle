package io.github.ibuildthecloud.dstack.api.html;

import com.netflix.config.DynamicStringProperty;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.gdapi.response.impl.DefaultHtmlTemplate;

public class ConfigBasedHtmlTemplate extends DefaultHtmlTemplate {

    private static final DynamicStringProperty JS_URL = ArchaiusUtil.getStringProperty("api.ui.js.url");
    private static final DynamicStringProperty CSS_URL = ArchaiusUtil.getStringProperty("api.ui.css.url");

    @Override
    public String getJsUrl() {
        return JS_URL.get();
    }

    @Override
    public String getCssUrl() {
        return CSS_URL.get();
    }

}
