package io.cattle.platform.extension.dynamic.dao;

import io.cattle.platform.core.model.ExternalHandler;
import io.cattle.platform.extension.dynamic.data.ExternalHandlerData;

import java.util.List;

public interface ExternalHandlerDao {

    List<? extends ExternalHandler> getExternalHandler(String processName);
    List<? extends ExternalHandlerData> getExternalHandlerData(String processName);

}
