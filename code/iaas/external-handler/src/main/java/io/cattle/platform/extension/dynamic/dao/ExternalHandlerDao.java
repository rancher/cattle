package io.cattle.platform.extension.dynamic.dao;

import io.cattle.platform.extension.dynamic.data.ExternalHandlerData;

import java.util.List;

public interface ExternalHandlerDao {

    List<? extends ExternalHandlerData> getExternalHandlerData(String processName);

}
