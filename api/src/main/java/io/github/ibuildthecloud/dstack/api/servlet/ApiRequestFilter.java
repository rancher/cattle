package io.github.ibuildthecloud.dstack.api.servlet;

import io.github.ibuildthecloud.gdapi.servlet.ApiRequestFilterDelegate;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.cloudstack.spring.module.web.ModuleBasedFilter;

public class ApiRequestFilter extends ModuleBasedFilter {

    ApiRequestFilterDelegate delegate;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        delegate.doFilter(request, response, chain);
    }

    public ApiRequestFilterDelegate getDelegate() {
        return delegate;
    }

    @Inject
    public void setDelegate(ApiRequestFilterDelegate delegate) {
        this.delegate = delegate;
    }

}
