package io.cattle.platform.eventing.annotation;

import java.lang.reflect.Method;
import java.util.List;

public interface EventNameProvider {

    List<String> events(EventHandler eventHandler, AnnotatedEventListener listener, Method method);

}
