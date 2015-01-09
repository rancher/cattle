package org.apache.cloudstack.managed.context.impl;

import org.apache.cloudstack.managed.context.ManagedContextListener;
import org.slf4j.MDC;

public class MdcClearListener implements ManagedContextListener<Object> {

    @Override
    public Object onEnterContext(boolean reentry) {
        return null;
    }

    @Override
    public void onLeaveContext(Object data, boolean reentry, Throwable t) {
        if (!reentry) {
            MDC.clear();
        }
    }

}
