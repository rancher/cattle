package io.cattle.platform.iaas.api.servlet.filter;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicStringProperty;

public class UIPathFilter implements Filter {

    public static final DynamicStringProperty STATIC_INDEX_HTML = ArchaiusUtil.getString("api.ui.index");

    private static final Logger log = LoggerFactory.getLogger(UIPathFilter.class);
    private static final String INDEX = "index.html";

    private byte[] indexCached = null;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
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

        return url != null && url.startsWith("http") && indexCached == null;
    }

    protected void reloadIndex() {
        String url = STATIC_INDEX_HTML.get();
        URL inputUrl;
        InputStream is = null;

        try {
            if (url != null && url.startsWith("http") && url.endsWith("index.html")) {
                inputUrl = new URL(url);
            } else {
                inputUrl = UIPathFilter.class.getClassLoader().getResource(INDEX);
            }

            is = inputUrl.openConnection().getInputStream();
            byte[] bytes = IOUtils.toByteArray(is);

            if (url != null && !url.endsWith("index.html")) {
                bytes = new String(bytes, "UTF-8").replaceAll("%PREFIX%", url).getBytes();
            }

            indexCached = bytes;
        } catch (IOException e) {
            log.error("Failed to load UI from [{}]", url, e);
            return;
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (((HttpServletRequest) request).getServletPath().indexOf('.') == -1) {
            if (shouldReload()) {
                reloadIndex();
            }

            if (indexCached == null) {
                request.getRequestDispatcher(STATIC_INDEX_HTML.get()).forward(request, response);
            } else {
                serveIndex((HttpServletResponse) response);
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    protected void serveIndex(HttpServletResponse response) throws IOException {
        OutputStream os = response.getOutputStream();

        response.setContentLength(indexCached.length);
        response.setContentType("text/html");

        os.write(indexCached);
    }

    @Override
    public void destroy() {
    }

}
