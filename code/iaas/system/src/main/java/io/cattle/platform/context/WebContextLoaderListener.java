package io.cattle.platform.context;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.apache.cloudstack.spring.module.factory.CloudStackSpringContext;
import org.apache.cloudstack.spring.module.web.CloudStackContextLoaderListener;

public class WebContextLoaderListener extends CloudStackContextLoaderListener {

    @Override
    protected CloudStackSpringContext constructCloudStackSpringContext(ServletContextEvent event) throws IOException {
        return new SpringContext();
    }

    @Override
    protected Class<?> determineContextClass(ServletContext servletContext) {
        String contextClassName = servletContext.getInitParameter(CONTEXT_CLASS_PARAM);
        if ( contextClassName == null ) {
            return DefaultedXmlWebApplicationContext.class;
        } else {
            return super.determineContextClass(servletContext);
        }
    }


}
