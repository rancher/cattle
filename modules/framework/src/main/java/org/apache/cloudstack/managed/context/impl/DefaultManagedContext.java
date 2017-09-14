/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.managed.context.impl;

import org.apache.cloudstack.managed.context.ManagedContext;
import org.apache.cloudstack.managed.context.ManagedContextListener;
import org.apache.cloudstack.managed.context.ManagedContextUtils;
import org.apache.cloudstack.managed.threadlocal.ManagedThreadLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultManagedContext implements ManagedContext {

    private static final Logger log = LoggerFactory.getLogger(DefaultManagedContext.class);

    List<ManagedContextListener<?>> listeners = new CopyOnWriteArrayList<>();

    public DefaultManagedContext(ManagedContextListener<?>... listeners) {
        if (listeners.length > 0) {
            this.listeners = Arrays.asList(listeners);
        }
    }

    @Override
    public void registerListener(ManagedContextListener<?> listener) {
        listeners.add(listener);
    }

    @Override
    public void runWithContext(final Runnable run) {
        try {
            callWithContext(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    run.run();
                    return null;
                }
            });
        } catch (Exception e) {
            /*
             * Only care about non-checked exceptions as the nature of runnable
             * prevents checked exceptions from happening
             */
            ManagedContextUtils.rethrowException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T callWithContext(Callable<T> callable) throws Exception {
        Object owner = new Object();

        Stack<ListenerInvocation> invocations = new Stack<>();
        boolean reentry = !ManagedContextUtils.setAndCheckOwner(owner);
        Throwable preError = null;
        Throwable logicError = null;

        try {
            for (ManagedContextListener<?> listener : listeners) {
                Object data = null;

                try {
                    data = listener.onEnterContext(reentry);
                } catch (Throwable t) {
                    /*
                     * If one listener fails, still call all other listeners and
                     * then we will call onLeaveContext for all
                     */
                    if (preError == null) {
                        preError = t;
                    }
                    log.error("Failed onEnterContext for listener [{}]", listener, t);
                }

                /*
                 * Stack data structure is used because in between onEnter and
                 * onLeave the listeners listSupport could have changed
                 */
                invocations.push(new ListenerInvocation((ManagedContextListener<Object>) listener, data));
            }

            try {
                if (preError == null) {
                    /*
                     * Only call if all the listeners didn't blow up on
                     * onEnterContext
                     */
                    return callable.call();
                } else {
                    throwException(preError);
                    return null;
                }
            } catch (Throwable t) {
                if (preError != t) {
                    logicError = t;
                }
                throwException(t);
                /*
                 * The below line should never be hit, but put here to make the
                 * compiler happy
                 */
                return null;
            } finally {
                Throwable postError = null;

                while (!invocations.isEmpty()) {
                    ListenerInvocation invocation = invocations.pop();
                    try {
                        invocation.listener.onLeaveContext(invocation.data, reentry, logicError == null ? preError : logicError);
                    } catch (Throwable t) {
                        postError = t;
                        log.error("Failed onLeaveContext for listener [{}]", invocation.listener, t);
                    }
                }

                if (preError == null && postError != null) {
                    throwException(postError);
                }
            }
        } finally {
            if (ManagedContextUtils.clearOwner(owner))
                ManagedThreadLocal.reset();
        }
    }

    protected void throwException(Throwable t) throws Exception {
        ManagedContextUtils.rethrowException(t);
        if (t instanceof Exception) {
            throw (Exception) t;
        }
    }

    public List<ManagedContextListener<?>> getListeners() {
        return listeners;
    }

    private static class ListenerInvocation {
        ManagedContextListener<Object> listener;
        Object data;

        public ListenerInvocation(ManagedContextListener<Object> listener, Object data) {
            super();
            this.listener = listener;
            this.data = data;
        }
    }
}
