package io.github.ibuildthecloud.dstack.context;

import java.io.IOException;

import javax.servlet.ServletContextEvent;

import org.apache.cloudstack.spring.module.factory.CloudStackSpringContext;

public class SpringContext extends CloudStackSpringContext {
    public SpringContext() throws IOException {
        super("META-INF/dstack", "bootstrap");
    }

    protected CloudStackSpringContext constructCloudStackSpringContext(ServletContextEvent event) throws IOException {
        return new CloudStackSpringContext();
    }

}
