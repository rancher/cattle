package io.github.ibuildthecloud.dstack.extension.spring;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import io.github.ibuildthecloud.dstack.extension.impl.ExtensionManagerImpl;
import io.github.ibuildthecloud.dstack.util.type.NamedUtils;
import io.github.ibuildthecloud.dstack.util.type.ScopeUtils;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class ExtensionDiscovery implements BeanPostProcessor {

    ExtensionManagerImpl extensionManager;
    Class<?> typeClass;
    String key;
    boolean respectScope;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if ( typeClass.isAssignableFrom(bean.getClass()) ) {
            extensionManager.addObject(getKey(bean), typeClass, bean, getName(bean, beanName));
        }

        return bean;
    }

    protected String getName(Object obj, String beanName) {
        if ( beanName != null && beanName.length() > 0 && Character.isUpperCase(beanName.charAt(0)) ) {
            return beanName;
        }

        return NamedUtils.getName(obj);
    }

    protected String getKey(Object obj) {
        if ( respectScope ) {
            return ScopeUtils.getDefaultScope(obj);
        } else {
            return key;
        }
    }

    @PostConstruct
    public void init() {
        if ( key == null && ! respectScope ) {
            throw new IllegalArgumentException("If respectScope is false, key must be set");
        }
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    public ExtensionManagerImpl getExtensionManager() {
        return extensionManager;
    }

    @Inject
    public void setExtensionManager(ExtensionManagerImpl extensionManager) {
        this.extensionManager = extensionManager;
    }

    public Class<?> getTypeClass() {
        return typeClass;
    }

    @Inject
    public void setTypeClass(Class<?> typeClass) {
        this.typeClass = typeClass;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public boolean isRespectScope() {
        return respectScope;
    }

    public void setRespectScope(boolean respectScope) {
        this.respectScope = respectScope;
    }

}
