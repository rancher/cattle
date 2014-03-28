package io.cattle.platform.extension.dynamic.dao;

import java.util.List;

import io.cattle.platform.core.model.ExternalHandler;

public interface ExternalHandlerDao {

    List<? extends ExternalHandler> getExternalHandler(String eventName);

}
