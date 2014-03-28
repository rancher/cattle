package io.cattle.platform.configitem.model;

public interface Client {

    Class<?> getResourceType();

    long getResourceId();
}
