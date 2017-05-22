package io.cattle.platform.engine.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.cloudstack.managed.threadlocal.ManagedThreadLocal;

public class OnDoneActions {

    private static ManagedThreadLocal<Context> TL = new ManagedThreadLocal<Context>() {
        @Override
        protected Context initialValue() {
            return new Context();
        }
    };

    public static void push() {
        Context c = TL.get();
        c.history.push(c.current);
        c.current = new ArrayList<>();
    }

    public static void pop() {
        Context c = TL.get();
        c.current = c.history.pop();
    }

    public static void add(Runnable run) {
        Context c = TL.get();
        c.current.add(run);
    }

    public static void runAndClear() {
        Context c = TL.get();
        for (Runnable run : c.current) {
            run.run();
        }
        c.current.clear();
    }

    private static class Context {
        Stack<List<Runnable>> history = new Stack<>();
        List<Runnable> current = new ArrayList<>();
    }

}
