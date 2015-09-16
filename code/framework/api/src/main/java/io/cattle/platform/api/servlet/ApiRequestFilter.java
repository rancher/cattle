package io.cattle.platform.api.servlet;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.metrics.util.MetricsUtil;
import io.cattle.platform.util.exception.ExceptionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.servlet.ApiRequestFilterDelegate;
import io.github.ibuildthecloud.gdapi.util.RequestUtils;
import io.github.ibuildthecloud.gdapi.version.Versions;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.spring.module.web.ModuleBasedFilter;
import com.codahale.metrics.Timer;
import com.netflix.config.DynamicStringListProperty;
import com.netflix.config.DynamicStringProperty;

public class ApiRequestFilter extends ModuleBasedFilter {

    private static final String DEFAULT_MODULE = "api-server";
    private static final DynamicStringListProperty IGNORE = ArchaiusUtil.getList("api.ignore.paths");
    private static final DynamicStringProperty INDEX = ArchaiusUtil.getString("api.ui.index");

    ApiRequestFilterDelegate delegate;
    Versions versions;
    Map<String, Timer> timers = new ConcurrentHashMap<String, Timer>();

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest)request;
        String path = httpRequest.getServletPath();

        boolean ignore = false;
        for (String prefix : IGNORE.get()) {
            if (path.startsWith(prefix)) {
                ignore = true;
                break;
            }
        }

        if (ignore) {
            chain.doFilter(request, response);
            return;
        }

        if (isLocalUI(httpRequest, path)) {
            if (path.contains(".")) {
                chain.doFilter(request, response);
                return;
            } else {
                RequestDispatcher rd = request.getRequestDispatcher("/index.html");
                rd.forward(request, response);
                return;
            }
        }

        try {
            new ManagedContextRunnable() {
                @Override
                protected void runInContext() {
                    long start = System.currentTimeMillis();
                    boolean success = false;
                    ApiContext context = null;
                    try {
                        context = delegate.doFilter(request, response, chain);
                        success = true;
                    } catch (IOException e) {
                        throw new WrappedException(e);
                    } catch (ServletException e) {
                        throw new WrappedException(e);
                    } finally {
                        done(context, start, success);
                    }
                }
            }.run();
        } catch (WrappedException e) {
            Throwable t = e.getCause();
            ExceptionUtils.rethrow(t, IOException.class);
            ExceptionUtils.rethrow(t, ServletException.class);
            ExceptionUtils.rethrowExpectedRuntime(t);
        }
    }

    protected void done(ApiContext context, long start, boolean success) {
        if (context == null) {
            return;
        }

        ApiRequest request = context.getApiRequest();
        if (request == null) {
            return;
        }

        if (request.getResponseCode() >= 400) {
            success = false;
        }

        long duration = System.currentTimeMillis() - start;
        if (request != null) {
            String key = String.format("api.%s.%s.%s", success ? "success" : "failed", request.getType(), request.getMethod().toLowerCase());

            Timer timer = timers.get(key);
            if (timer == null) {
                timer = MetricsUtil.getRegistry().timer(key);
                timers.put(key, timer);
            }
            timer.update(duration, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    protected String getModule(FilterConfig filterConfig) {
        String result = super.getModule(filterConfig);
        return result == null ? DEFAULT_MODULE : result;
    }

    protected boolean isLocalUI(HttpServletRequest request, String path) {
        if (!"local".equals(INDEX.get())) {
            return false;
        }

        if ("/".equals(path)) {
            return RequestUtils.isBrowser(request, false);
        }

        boolean found = false;
        for (String version : versions.getVersions()) {
            if (path.startsWith("/" + version)) {
                found = true;
                break;
            }
        }

        return !found;
    }

    public ApiRequestFilterDelegate getDelegate() {
        return delegate;
    }

    @Inject
    public void setDelegate(ApiRequestFilterDelegate delegate) {
        this.delegate = delegate;
    }

    public Versions getVersions() {
        return versions;
    }

    @Inject
    public void setVersions(Versions versions) {
        this.versions = versions;
    }

    private static final class WrappedException extends RuntimeException {
        private static final long serialVersionUID = 8188803805854482331L;

        public WrappedException(Throwable cause) {
            super(cause);
        }
    }

}
