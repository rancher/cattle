package io.github.ibuildthecloud.dstack.extension.spring;

import io.github.ibuildthecloud.dstack.engine.handler.ProcessHandler;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessLogic;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessPostListener;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessPreListener;

public class ProcessExtensionDiscovery extends ExtensionDiscovery {

    @Override
    protected String[] getKeys(Object obj) {
        if ( obj instanceof ProcessLogic ) {
            String[] names = ((ProcessLogic)obj).getProcessNames();
            String[] result = new String[names.length];

            for ( int i = 0 ; i < result.length ; i++ ) {
                String suffix = null;
                if ( obj instanceof ProcessHandler ) {
                    suffix = ".handlers";
                } else if ( obj instanceof ProcessPreListener ) {
                    suffix = ".pre.listeners";
                } else if ( obj instanceof ProcessPostListener ) {
                    suffix = ".post.listeners";
                } else {
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
