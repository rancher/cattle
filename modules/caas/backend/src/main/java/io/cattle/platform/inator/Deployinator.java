package io.cattle.platform.inator;

public interface Deployinator {

    Result reconcile(Class<?> clz, Long id);

}
