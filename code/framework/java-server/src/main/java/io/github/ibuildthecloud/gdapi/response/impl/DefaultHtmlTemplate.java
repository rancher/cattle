package io.github.ibuildthecloud.gdapi.response.impl;

import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.HtmlTemplate;
import io.github.ibuildthecloud.gdapi.util.Settings;
import io.github.ibuildthecloud.gdapi.util.SettingsUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import javax.annotation.PostConstruct;
import javax.servlet.http.Cookie;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class DefaultHtmlTemplate implements HtmlTemplate {

    String header;
    byte[] footer;
    String jsUrl, cssUrl;
    Settings settings;

    @Override
    public byte[] getHeader(ApiRequest request, Object response) {
        try {
            return getStringHeader(request, response).getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    protected String getStringHeader(ApiRequest request, Object response) {
        String result = header;

        URL schemaUrl = ApiContext.getUrlBuilder().resourceCollection(Schema.class);
        if (schemaUrl == null) {
            result = result.replace("%SCHEMAS%", "");
        } else {
            result = result.replace("%SCHEMAS%", schemaUrl.toExternalForm());
        }

        if ("true".equals(SettingsUtil.getSetting(settings, "api.dev", ""))) {
            Cookie[] cookies = request.getServletContext().getRequest().getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("js.url".equals(cookie.getName()) && !StringUtils.isEmpty(cookie.getValue())) {
                        result = result.replace("%JS%", cookie.getValue());
                    }
                    if ("css.url".equals(cookie.getName()) && !StringUtils.isEmpty(cookie.getValue())) {
                        result = result.replace("%CSS%", cookie.getValue());
                    }
                }
            }
        }

        result = result.replace("%JS%", SettingsUtil.getSetting(settings, "api.js.url", getJsUrl()));
        result = result.replace("%CSS%", SettingsUtil.getSetting(settings, "api.css.url", getCssUrl()));

        String user = getUser(request, response);
        if (user == null) {
            user = "";
        }

        result = result.replace("%USER%", user);

        return result;
    }

    protected String getUser(ApiRequest request, Object response) {
        return null;
    }

    @Override
    public byte[] getFooter(ApiRequest request, Object response) {
        return footer;
    }

    @PostConstruct
    public void init() throws IOException {
        InputStream is = null;

        try {
            is = this.getClass().getResourceAsStream("header.txt");
            if (is == null) {
                is = DefaultHtmlTemplate.class.getResourceAsStream("header.txt");
            }
            header = IOUtils.toString(is);
        } finally {
            IOUtils.closeQuietly(is);
        }

        try {
            is = this.getClass().getResourceAsStream("footer.txt");
            if (is == null) {
                is = DefaultHtmlTemplate.class.getResourceAsStream("footer.txt");
            }
            footer = IOUtils.toByteArray(is);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public String getJsUrl() {
        return jsUrl;
    }

    public void setJsUrl(String jsUrl) {
        this.jsUrl = jsUrl;
    }

    public String getCssUrl() {
        return cssUrl;
    }

    public void setCssUrl(String cssUrl) {
        this.cssUrl = cssUrl;
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }
}
