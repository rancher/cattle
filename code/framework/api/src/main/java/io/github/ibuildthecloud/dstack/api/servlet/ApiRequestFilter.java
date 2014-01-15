package io.github.ibuildthecloud.dstack.api.servlet;

import io.github.ibuildthecloud.dstack.util.exception.ExceptionUtils;
import io.github.ibuildthecloud.gdapi.servlet.ApiRequestFilterDelegate;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.spring.module.web.ModuleBasedFilter;

public class ApiRequestFilter extends ModuleBasedFilter {

    private static final String DEFAULT_MODULE = "api-server";

    ApiRequestFilterDelegate delegate;

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException,
            ServletException {
        try {
            new ManagedContextRunnable() {
                @Override
                protected void runInContext() {
                    try {
                        delegate.doFilter(request, response, chain);
                    } catch (IOException e) {
                        throw new WrappedException(e);
                    } catch (ServletException e) {
                        throw new WrappedException(e);
                    }
                }
            }.run();
        } catch ( WrappedException e ) {
            Throwable t = e.getCause();
            ExceptionUtils.rethrow(t, IOException.class);
            ExceptionUtils.rethrow(t, ServletException.class);
            ExceptionUtils.rethrowExpectedRuntime(t);
        }
    }

    @Override
    protected String getModule(FilterConfig filterConfig) {
        String result = super.getModule(filterConfig);
        return result == null ? DEFAULT_MODULE : result;
    }

    public ApiRequestFilterDelegate getDelegate() {
        return delegate;
    }

    @Inject
    public void setDelegate(ApiRequestFilterDelegate delegate) {
        this.delegate = delegate;
    }

    private static final class WrappedException extends RuntimeException {
        private static final long serialVersionUID = 8188803805854482331L;

        public WrappedException(Throwable cause) {
            super(cause);
        }
    }
}
