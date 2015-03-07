package io.cattle.platform.extension.spring;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class ParentExtensionDiscovery implements BeanPostProcessor, ApplicationContextAware {

    ApplicationContext applicationContext;
    Set<ExtensionDiscovery> extensions = new HashSet<ExtensionDiscovery>();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        for (ExtensionDiscovery discovery : extensions) {
            discovery.postProcessBeforeInitialization(bean, beanName);
        }

        return bean;
    }

    @PostConstruct
    public void init() {
        ApplicationContext parent = applicationContext.getParent();
        while (parent != null) {
            extensions.addAll(parent.getBeansOfType(ExtensionDiscovery.class).values());
            parent = parent.getParent();
        }
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
