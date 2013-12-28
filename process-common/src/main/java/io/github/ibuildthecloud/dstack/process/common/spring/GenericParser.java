package io.github.ibuildthecloud.dstack.process.common.spring;

import java.beans.PropertyDescriptor;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class GenericParser implements BeanDefinitionParser {

    private static AtomicInteger COUNTER = new AtomicInteger();

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        String id = element.getAttribute("id");
        if ( StringUtils.isBlank(id) ) {
            String name = element.getAttribute("name");
            if ( StringUtils.isBlank(name) ) {
                id = "generated$" + GenericProcessFactory.class.getSimpleName() + "$" + COUNTER.incrementAndGet();
            } else {
                id = "generated$" + name;
            }
        }

        BeanDefinitionBuilder factoryBuilder = BeanDefinitionBuilder.rootBeanDefinition(GenericProcessFactory.class);

        for ( PropertyDescriptor prop : PropertyUtils.getPropertyDescriptors(GenericProcessFactory.class) ) {
            if ( prop.getWriteMethod() != null && prop.getReadMethod() != null ) {
                String name = prop.getName();
                factoryBuilder.addPropertyValue(name, element.getAttribute(name));
            }
        }

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition();
        builder.getRawBeanDefinition().setFactoryBeanName(id + "$factory");
        builder.getRawBeanDefinition().setFactoryMethodName("getObject");


        /* You might be thinking it's silly to use a factory and then a bean referring to the factory
         * and to not use a Spring FactoryBean.  There is actually a reason for it.
         * io.github.ibuildthecloud.dstack.extension.spring.ExtensionDiscovery is a BeanPostProcessor
         * and is discovers beans by looking at the type of the beans passed to it.  If you use a
         * Spring FactoryBean the created bean is not passed to BeanPostProcessor but instead the Spring
         * FactoryBean.  This makes it very difficult to find the beans we want when using Spring FactoryBeans
         */
        register(id, builder, parserContext);
        return register(id + "$factory", factoryBuilder, parserContext);
    }

    protected BeanDefinition register(String id, BeanDefinitionBuilder builder, ParserContext parserContext) {
        BeanDefinition def = builder.getBeanDefinition();
        BeanDefinitionHolder holder = new BeanDefinitionHolder(def, id);
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, parserContext.getRegistry());

        return def;
    }

}