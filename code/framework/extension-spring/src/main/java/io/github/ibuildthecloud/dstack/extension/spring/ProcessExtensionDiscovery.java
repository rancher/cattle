package io.github.ibuildthecloud.dstack.extension.spring;

import java.util.HashMap;
import java.util.Map;

import io.github.ibuildthecloud.dstack.engine.handler.ProcessHandler;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessLogic;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessPostListener;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessPreListener;

public class ProcessExtensionDiscovery extends ExtensionDiscovery {

    private static final Map<Class<?>, String> SUFFIXES = new HashMap<Class<?>,String>();
    static {
        SUFFIXES.put(ProcessHandler.class, ".handlers");
        SUFFIXES.put(ProcessPreListener.class, ".pre.listeners");
        SUFFIXES.put(ProcessPostListener.class, ".post.listeners");
    }

    @Override
    protected String[] getKeys(Object obj) {
        if ( obj instanceof ProcessLogic ) {
            String[] names = ((ProcessLogic)obj).getProcessNames();
            String[] result = new String[names.length];

            for ( int i = 0 ; i < result.length ; i++ ) {
                String suffix = SUFFIXES.get(getTypeClass());

                if ( suffix == null ) {
                    throw new IllegalArgumentException("Object is not an instance of ProcessHandler, "
                            + "ProcessPreListener, or ProcessPostListener, got [" + obj.getClass() + "]");
                }

                result[i] = "process." + names[i] + suffix;
            }

            return result;
        }

        return new String[0];
    }

}
