package io.cattle.platform.iaas.api.servlet.filter;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.netflix.config.DynamicStringProperty;

public class UIPathFilter implements Filter {

    public static final DynamicStringProperty STATIC_INDEX_HTML = ArchaiusUtil.getString("api.ui.index");

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        if ( ((HttpServletRequest)request).getServletPath().indexOf('.') == -1 ) {
            request.getRequestDispatcher(STATIC_INDEX_HTML.get()).forward(request, response);
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
    }

}
