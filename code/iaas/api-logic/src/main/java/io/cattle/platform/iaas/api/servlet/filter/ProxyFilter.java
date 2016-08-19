package io.cattle.platform.iaas.api.servlet.filter;

import io.cattle.platform.iaas.api.request.handler.GenericWhitelistedProxy;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

public class ProxyFilter implements Filter {

    String proxy;
    boolean redirects = true;
    boolean parseform = false;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        proxy = filterConfig.getInitParameter("proxy");
        String value = filterConfig.getInitParameter("redirects");
        if (StringUtils.isNotBlank(value)) {
            redirects = Boolean.parseBoolean(value);
        }
        String parseFormValue = filterConfig.getInitParameter("parseform");
        if (StringUtils.isNotBlank(parseFormValue)) {
            parseform = Boolean.parseBoolean(parseFormValue);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        RequestDispatcher rd = request.getRequestDispatcher("/v1/proxy/" + proxy + ((HttpServletRequest)request).getRequestURI());
        request.setAttribute(GenericWhitelistedProxy.ALLOWED_HOST, true);
        request.setAttribute(GenericWhitelistedProxy.SET_HOST_CURRENT_HOST, true);
        request.setAttribute(GenericWhitelistedProxy.REDIRECTS, redirects);
        request.setAttribute(GenericWhitelistedProxy.PARSE_FORM, parseform);

        rd.forward(request, response);
        return;
    }

    @Override
    public void destroy() {
    }

}
