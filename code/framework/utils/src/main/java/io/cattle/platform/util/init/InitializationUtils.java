package io.cattle.platform.util.init;


public class InitializationUtils {

//    public static void onInitialization(final Object thisObj, final Object... objs) {
//        try {
//            final List<Method> methods = new ArrayList<Method>();
//            for ( Method method : thisObj.getClass().getDeclaredMethods() ) {
//                AfterExtensionInitialization init = method.getAnnotation(AfterExtensionInitialization.class);
//                if ( init != null && method.getParameterTypes().length == 0 ) {
//                    method.setAccessible(true);
//                    methods.add(method);
//                }
//            }
//
//            if ( methods.size() == 0 ) {
//                throw new IllegalArgumentException("No methods with @AfterExtensionInitialization are found on [" + thisObj + "]");
//            }
//
//            onInitialization(thisObj, new Runnable() {
//                @Override
//                public void run() {
//                    for ( Method method : methods ) {
//                        try {
//                            method.invoke(thisObj);
//                        } catch (IllegalArgumentException e) {
//                            throw new IllegalStateException("Failed to initialize [" + method + "]", e);
//                        } catch (IllegalAccessException e) {
//                            throw new IllegalStateException("Failed to initialize [" + method + "]", e);
//                        } catch (InvocationTargetException e) {
//                            throw new IllegalStateException("Failed to initialize [" + method + "]", e);
//                        }
//                    }
//                }
//            }, objs);
//        } catch (SecurityException e) {
//            throw new IllegalStateException("Failed to initialize [" + thisObj + "]", e);
//        }
//    }
//
//    public static void onInitialization(Object thisObj, Runnable runnable, Object... objs) {
//        runnable.run();
//    }

}
