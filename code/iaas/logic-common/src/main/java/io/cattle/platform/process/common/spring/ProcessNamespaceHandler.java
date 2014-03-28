package io.cattle.platform.process.common.spring;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class ProcessNamespaceHandler extends NamespaceHandlerSupport {

    @Override
    public void init() {
        registerBeanDefinitionParser("process", new ProcessParser());
        registerBeanDefinitionParser("defaultProcesses", new DefaultProcessesParser());
    }

}
