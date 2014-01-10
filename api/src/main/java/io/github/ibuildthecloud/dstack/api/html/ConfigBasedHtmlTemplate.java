package io.github.ibuildthecloud.dstack.api.html;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.gdapi.response.impl.DefaultHtmlTemplate;

import com.netflix.config.DynamicStringProperty;

public class ConfigBasedHtmlTemplate extends DefaultHtmlTemplate {

    private static final DynamicStringProperty JS_URL = ArchaiusUtil.getString("api.ui.js.url");
    private static final DynamicStringProperty CSS_URL = ArchaiusUtil.getString("api.ui.css.url");

    @Override
    public String getJsUrl() {
        return JS_URL.get();
    }

    @Override
    public String getCssUrl() {
        return CSS_URL.get();
    }

}
