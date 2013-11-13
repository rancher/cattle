package io.github.ibuildthecloud.dstack.context;

import java.io.IOException;

import javax.servlet.ServletContextEvent;

import org.apache.cloudstack.spring.module.factory.CloudStackSpringContext;
import org.apache.cloudstack.spring.module.web.CloudStackContextLoaderListener;

public class WebContextLoaderListener extends CloudStackContextLoaderListener {

    @Override
    protected CloudStackSpringContext constructCloudStackSpringContext(ServletContextEvent event) throws IOException {
        return new SpringContext();
    }

}
