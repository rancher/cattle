package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.StoragePool;

import java.util.List;
import java.util.Map;

public interface StoragePoolDao {

    List<? extends StoragePool> findExternalActivePools();

    StoragePool mapNewPool(Host host, Map<String,Object> properties);

}
