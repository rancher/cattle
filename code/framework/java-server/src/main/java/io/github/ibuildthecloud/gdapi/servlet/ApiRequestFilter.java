package io.github.ibuildthecloud.gdapi.servlet;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class ApiRequestFilter implements Filter {

    ApiRequestFilterDelegate apiRequestFilterDelegate;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        apiRequestFilterDelegate.doFilter(request, response, chain);
    }

    @Override
    public void destroy() {
    }

    public ApiRequestFilterDelegate getApiRequestFilterDelegate() {
        return apiRequestFilterDelegate;
    }

    @Inject
    public void setApiRequestFilterDelegate(ApiRequestFilterDelegate apiRequestFilterDelegate) {
        this.apiRequestFilterDelegate = apiRequestFilterDelegate;
    }

}
