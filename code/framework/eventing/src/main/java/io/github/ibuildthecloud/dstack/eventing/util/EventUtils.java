package io.github.ibuildthecloud.dstack.eventing.util;

import io.github.ibuildthecloud.dstack.eventing.annotation.AnnotatedEventListener;
import io.github.ibuildthecloud.dstack.eventing.annotation.EventHandler;
import io.github.ibuildthecloud.dstack.util.type.NamedUtils;

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
