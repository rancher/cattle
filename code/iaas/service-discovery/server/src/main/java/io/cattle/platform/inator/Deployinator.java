package io.cattle.platform.inator;

import io.cattle.platform.eventing.annotation.AnnotatedEventListener;

public interface Deployinator extends AnnotatedEventListener {

    Result reconcile(Class<?> clz, Long id);

}
