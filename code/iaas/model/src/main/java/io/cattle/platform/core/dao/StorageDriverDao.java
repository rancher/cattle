package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.StorageDriver;

public interface StorageDriverDao {

    StorageDriver findSecretsDriver(long accountId);

    void createSecretsVolume(Instance instance, StorageDriver storageDriver, String token);

}
