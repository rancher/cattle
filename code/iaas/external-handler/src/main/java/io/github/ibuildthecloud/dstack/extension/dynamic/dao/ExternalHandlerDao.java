package io.github.ibuildthecloud.dstack.extension.dynamic.dao;

import java.util.List;

import io.github.ibuildthecloud.dstack.core.model.ExternalHandler;

public interface ExternalHandlerDao {

    List<? extends ExternalHandler> getExternalHandler(String eventName);

}
