package io.cattle.platform.eventing.util;

import io.cattle.platform.eventing.annotation.AnnotatedEventListener;
import io.cattle.platform.eventing.annotation.EventHandler;
import io.cattle.platform.util.type.NamedUtils;

import java.lang.reflect.Method;

import org.apache.commons.lang.StringUtils;


public class EventUtils {

    public static String getEventNameNonProvided(EventHandler handler, AnnotatedEventListener listener, Method method) {
        String name = handler.name();
        if ( StringUtils.isEmpty(name) ) {
            return NamedUtils.toDotSeparated(method.getName());
        } else {
            return name;
        }
    }

}
