package io.cattle.platform.extension.dynamic.dao;

import io.cattle.platform.core.model.ExternalHandler;

import java.util.List;

public interface ExternalHandlerDao {

    List<? extends ExternalHandler> getExternalHandler(String eventName);

}
