package io.cattle.platform.api.servlet;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import javax.annotation.PostConstruct;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicStringProperty;

public class IndexFile {

    public static final DynamicStringProperty STATIC_INDEX_HTML = ArchaiusUtil.getString("api.ui.index");

    private static final Logger log = LoggerFactory.getLogger(IndexFile.class);
    private static final String LOCAL = "local";

    private byte[] indexCached = null;

    @PostConstruct
    public void init() {
        reloadIndex();
        STATIC_INDEX_HTML.addCallback(new Runnable() {
            @Override
            public void run() {
                reloadIndex();
            }
        });
    }

    protected boolean shouldReload() {
        String url = STATIC_INDEX_HTML.get();
        return url != null && !url.equalsIgnoreCase(LOCAL) && indexCached == null;
    }

    protected void reloadIndex() {
        String url = STATIC_INDEX_HTML.get();
        URL inputUrl = null;
        InputStream is = null;

        try {
            if (LOCAL.equals(STATIC_INDEX_HTML.get())) {
                indexCached = null;
                return;
            }

            if (url != null) {
                if (url.startsWith("//")) {
                    url = "https:" + url;
                }
                if (!url.endsWith("index.html")) {
                    if (!url.endsWith("/")) {
                        url += "/";
                    }
                    url += "index.html";
                }
                inputUrl = new URL(url);
            }

            if (inputUrl == null) {
                indexCached = null;
                return;
            }

            is = inputUrl.openConnection().getInputStream();
            indexCached = IOUtils.toByteArray(is);
        } catch (IOException e) {
            log.error("Failed to load UI from [{}]", url, e);
            return;
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public boolean canServeContent() {
        return indexCached != null || LOCAL.equals(STATIC_INDEX_HTML.get());
    }

    public void serveIndex(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (shouldReload()) {
            reloadIndex();
        }

        if (LOCAL.equalsIgnoreCase(STATIC_INDEX_HTML.get())) {
            response.addHeader("Cache-Control", "max-age=0, no-cache");
            RequestDispatcher rd = request.getRequestDispatcher("/index.html");
            rd.forward(request, response);
            return;
        }

        if (indexCached == null) {
            return;
        }

        OutputStream os = response.getOutputStream();

        response.addHeader("Cache-Control", "max-age=0, no-cache");
        response.setContentLength(indexCached.length);
        response.setContentType("text/html");

        os.write(indexCached);
    }
}