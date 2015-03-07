package io.cattle.platform.configitem.spring;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

public class URLArrayFactory implements FactoryBean<URL[]>, ApplicationContextAware {

    ApplicationContext applicationContext;
    String[] locations;

    @Override
    public URL[] getObject() throws Exception {
        List<URL> resources = new ArrayList<URL>();

        for (String location : locations) {
            for (Resource resource : applicationContext.getResources(location)) {
                resources.add(resource.getURL());
            }
        }

        return resources.toArray(new URL[resources.size()]);
    }

    @Override
    public Class<?> getObjectType() {
        return URL[].class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public String[] getLocations() {
        return locations;
    }

    public void setLocations(String[] locations) {
        this.locations = locations;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
