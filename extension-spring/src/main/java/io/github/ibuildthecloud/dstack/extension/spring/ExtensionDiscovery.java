package io.github.ibuildthecloud.dstack.extension.spring;

import io.github.ibuildthecloud.dstack.extension.impl.ExtensionManagerImpl;
import io.github.ibuildthecloud.dstack.util.type.NamedUtils;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class ExtensionDiscovery implements BeanPostProcessor {

    ExtensionManagerImpl extensionManager;
    Class<?> typeClass;
    String key;
    String[] keys;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if ( typeClass.isAssignableFrom(bean.getClass()) ) {
            for ( String key : getKeys(bean) ) {
                extensionManager.addObject(key, typeClass, bean, getName(bean, beanName));
            }
        }

        return bean;
    }

    protected String getName(Object obj, String beanName) {
        if ( beanName != null && beanName.length() > 0 && Character.isUpperCase(beanName.charAt(0)) ) {
            return beanName;
        }

        return NamedUtils.getName(obj);
    }

    protected String[] getKeys(Object obj) {
        return keys;
    }

    @PostConstruct
    public void init() {
        if ( key == null && typeClass != null ) {
            key = NamedUtils.toDotSeparated(typeClass.getSimpleName());
        }

        if ( key != null ) {
            keys = new String[] { key };
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

}
