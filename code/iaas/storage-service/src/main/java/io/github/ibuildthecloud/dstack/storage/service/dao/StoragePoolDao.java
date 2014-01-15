package io.github.ibuildthecloud.dstack.storage.service.dao;

import io.github.ibuildthecloud.dstack.core.model.StoragePool;

import java.util.List;

public interface StoragePoolDao {

    List<? extends StoragePool> findExternalActivePools();

}
