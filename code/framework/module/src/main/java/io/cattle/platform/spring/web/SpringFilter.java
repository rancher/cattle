package io.cattle.platform.spring.web;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import org.springframework.web.context.support.WebApplicationContextUtils;

public abstract class SpringFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        WebApplicationContextUtils
            .getRequiredWebApplicationContext(filterConfig.getServletContext())
            .getAutowireCapableBeanFactory()
            .autowireBean(this);
    }

    @Override
    public void destroy() {
    }

}
