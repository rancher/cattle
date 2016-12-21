package io.cattle.platform.extension.spring;

import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.engine.handler.ProcessLogic;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.extension.impl.ExtensionManagerImpl;
import io.cattle.platform.util.type.NamedUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtensionDiscovery {

    private static final Logger log = LoggerFactory.getLogger("ConsoleStatus");

    @Inject
    ExtensionManagerImpl extensionManager;
    @Inject
    List<ProcessHandler> processHandlers;
    @Inject
    List<ProcessPreListener> processPreListeners;
    @Inject
    List<ProcessPostListener> processPostListeners;

    private static final Map<Class<?>, String> SUFFIXES = new HashMap<Class<?>, String>();
    static {
        SUFFIXES.put(ProcessHandler.class, ".handlers");
        SUFFIXES.put(ProcessPreListener.class, ".pre.listeners");
        SUFFIXES.put(ProcessPostListener.class, ".post.listeners");
    }

    @PostConstruct
    public void init() {
        log.info("Loading processes");

        for (ProcessHandler handler : processHandlers) {
            process(handler, ProcessHandler.class);
        }
        for (ProcessPreListener handler : processPreListeners) {
            process(handler, ProcessPreListener.class);
        }
        for (ProcessPostListener handler : processPostListeners) {
            process(handler, ProcessPostListener.class);
        }
        extensionManager.reset();
    }

    protected String[] getKeys(Object obj, Class<?> typeClz) {
        if (obj instanceof ProcessLogic) {
            String[] names = ((ProcessLogic) obj).getProcessNames();
            String[] result = new String[names.length];

            for (int i = 0; i < result.length; i++) {
                String suffix = SUFFIXES.get(typeClz);

                if (suffix == null) {
                    throw new IllegalArgumentException("Object is not an instance of ProcessHandler, " + "ProcessPreListener, or ProcessPostListener, got ["
                            + obj.getClass() + "]");
                }

                result[i] = "process." + names[i].toLowerCase() + suffix;
            }

            return result;
        }

        return new String[0];
    }

    public Object process(Object bean, Class<?> typeClass) {
        String name = NamedUtils.getName(bean);
        if (name != null) {
            for (String key : getKeys(bean, typeClass)) {
                extensionManager.addObject(key, typeClass, bean, name);
            }
        }

        return bean;
    }

}