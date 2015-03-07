package org.apache.cloudstack.spring.module.context;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.Resource;

public class TimedXmlBeanDefinitionReader extends XmlBeanDefinitionReader {

    private static long time = 0;

    public TimedXmlBeanDefinitionReader(BeanDefinitionRegistry registry) {
        super(registry);
    }

    @Override
    public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
        long start = System.currentTimeMillis();
        try {
            return super.loadBeanDefinitions(resource);
        } finally {
            long current = System.currentTimeMillis() - start;
            time += current;
            logger.debug("Reader processing time [" + current + "] ms, total [" + time + "] ms");
        }
    }

}
