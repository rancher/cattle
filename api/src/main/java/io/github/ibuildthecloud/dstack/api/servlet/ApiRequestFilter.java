package io.github.ibuildthecloud.dstack.api.servlet;

import io.github.ibuildthecloud.gdapi.servlet.ApiRequestFilterDelegate;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.spring.module.web.ModuleBasedFilter;

public class ApiRequestFilter extends ModuleBasedFilter {

    ApiRequestFilterDelegate delegate;

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException,
            ServletException {
        new ManagedContextRunnable() {
            @Override
            protected void runInContext() {
                try {
                    delegate.doFilter(request, response, chain);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (ServletException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }.run();
    }

    public ApiRequestFilterDelegate getDelegate() {
        return delegate;
    }

    @Inject
    public void setDelegate(ApiRequestFilterDelegate delegate) {
        this.delegate = delegate;
    }

}
