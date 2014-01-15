package io.github.ibuildthecloud.dstack.deferred.context;

import io.github.ibuildthecloud.dstack.deferred.util.DeferredUtils;

import org.apache.cloudstack.managed.context.ManagedContextListener;

public class DeferredContextListener implements ManagedContextListener<Object> {

    @Override
    public Object onEnterContext(boolean reentry) {
        return null;
    }

    @Override
    public void onLeaveContext(Object data, boolean reentry, Throwable t) {
        if ( ! reentry && t == null ) {
            DeferredUtils.runDeferred();
        }
    }

}
