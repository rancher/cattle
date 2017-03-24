package io.cattle.platform.iaas.api.servlet.filter;

import io.cattle.platform.iaas.api.request.handler.GenericWhitelistedProxy;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
    Set<String> roles;
    Set<String> methods;

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
        String roles = filterConfig.getInitParameter("roles");
        if (StringUtils.isNotBlank(roles)) {
            this.roles = new HashSet<>(Arrays.asList(roles.trim().split("\\s*,\\s*")));
        }
        String methods = filterConfig.getInitParameter("rolesMethods");
        if (StringUtils.isNotBlank(roles)) {
            this.methods = new HashSet<>(Arrays.asList(methods.trim().split("\\s*,\\s*")));
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        RequestDispatcher rd = request.getRequestDispatcher("/v1/proxy/" + proxy + ((HttpServletRequest)request).getRequestURI());
        request.setAttribute(GenericWhitelistedProxy.ALLOWED_HOST, true);
        request.setAttribute(GenericWhitelistedProxy.SET_HOST_CURRENT_HOST, true);
        request.setAttribute(GenericWhitelistedProxy.REDIRECTS, redirects);
        request.setAttribute(GenericWhitelistedProxy.PARSE_FORM, parseform);

        if (roles != null) {
            request.setAttribute(GenericWhitelistedProxy.REQUIRE_ROLE, roles);
        }
        if (methods != null) {
            request.setAttribute(GenericWhitelistedProxy.METHOD_ROLE, methods);
        }

        rd.forward(request, response);
        return;
    }

    @Override
    public void destroy() {
    }

}
