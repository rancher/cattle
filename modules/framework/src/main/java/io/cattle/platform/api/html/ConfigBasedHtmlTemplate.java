package io.cattle.platform.api.html;

import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.doc.TypeDocumentation;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.impl.DefaultHtmlTemplate;
import io.github.ibuildthecloud.gdapi.url.UrlBuilder;

import java.net.URL;

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

    @Override
    protected String getUser(ApiRequest request, Object response) {
        return ApiUtils.getPolicy().getUserName();
    }

    @Override
    protected String getStringHeader(ApiRequest request, Object response) {
        String result = super.getStringHeader(request, response);

        UrlBuilder builder = ApiContext.getUrlBuilder();
        URL link = builder.resourceCollection(TypeDocumentation.class);

        if (link != null) {
            result = result.replace("//BEFORE DATA", String.format("var docJson = '%s';", link.toExternalForm()));
        }

        return result;
    }

}
