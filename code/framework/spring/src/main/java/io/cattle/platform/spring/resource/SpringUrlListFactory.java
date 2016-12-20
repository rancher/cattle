package io.cattle.platform.spring.resource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

public class SpringUrlListFactory implements FactoryBean<List<URL>>, ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(SpringUrlListFactory.class);

    PathMatchingResourcePatternResolver resolver;
    List<String> resources;

    @Override
    public List<URL> getObject() {
        List<URL> result = new ArrayList<URL>();

        try {
            for (String resource : resources) {
                try {
                    for (Resource r : resolver.getResources(resource)) {
                        if (r.exists()) {
                            result.add(r.getURL());
                        } else {
                            log.debug("Skipping resource [{}]", r);
                        }
                    }
                } catch (FileNotFoundException fnfe) {
                    log.debug("Skipping resource [{}], not found", resource);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        return result;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        resolver = new PathMatchingResourcePatternResolver(applicationContext);
    }

    @Override
    public Class<?> getObjectType() {
        return List.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public List<String> getResources() {
        return resources;
    }

    public void setResources(List<String> resources) {
        this.resources = resources;
    }

}
