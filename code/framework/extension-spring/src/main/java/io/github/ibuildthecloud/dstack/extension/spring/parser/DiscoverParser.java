package io.github.ibuildthecloud.dstack.extension.spring.parser;

import io.github.ibuildthecloud.dstack.extension.spring.ExtensionDiscovery;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

public class DiscoverParser extends AbstractSingleBeanDefinitionParser implements BeanDefinitionParser {

    @Override
    protected Class<?> getBeanClass(Element element) {
        return ExtensionDiscovery.class;
    }

    @Override
    protected void doParse(Element element, BeanDefinitionBuilder builder) {
        String clz = element.getAttribute("class");
        String key = element.getAttribute("key");

        builder.addPropertyValue("typeClass", clz);
        if ( StringUtils.hasText(key) ) {
            builder.addPropertyValue("key", key);
        }

        super.doParse(element, builder);
    }

    @Override
    protected boolean shouldGenerateIdAsFallback() {
        return true;
    }

}
