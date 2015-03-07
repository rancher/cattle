package io.cattle.platform.spring.resource;

import io.cattle.platform.util.resource.ResourceLoader;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

public class SpringResourceLoader implements ResourceLoader, ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(SpringResourceLoader.class);
    public static final String PREFIX = "classpath*:";

    PathMatchingResourcePatternResolver resolver;

    @Override
    public List<URL> getResources(String path) throws IOException {
        List<URL> result = new ArrayList<URL>();

        for (Resource r : resolver.getResources(PREFIX + path)) {
            if (r.exists()) {
                result.add(r.getURL());
            } else {
                log.debug("Skipping resource [{}]", r);
            }
        }

        return result;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        resolver = new PathMatchingResourcePatternResolver(applicationContext);
    }

    public PathMatchingResourcePatternResolver getResolver() {
        return resolver;
    }

    public void setResolver(PathMatchingResourcePatternResolver resolver) {
        this.resolver = resolver;
    }

}
