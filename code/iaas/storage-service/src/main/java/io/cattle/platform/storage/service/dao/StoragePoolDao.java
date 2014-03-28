package io.cattle.platform.storage.service.dao;

import io.cattle.platform.core.model.StoragePool;

import java.util.List;

public interface StoragePoolDao {

    List<? extends StoragePool> findExternalActivePools();

}
