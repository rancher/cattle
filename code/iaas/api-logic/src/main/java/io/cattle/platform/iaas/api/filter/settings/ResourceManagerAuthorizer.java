package io.cattle.platform.iaas.api.filter.settings;

public interface ResourceManagerAuthorizer {

    Object authorize(Object object);

}
