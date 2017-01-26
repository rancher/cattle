package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.StorageDriver;
import io.cattle.platform.core.model.Volume;

public interface StorageDriverDao {

    StorageDriver findSecretsDriver(long accountId);

    Volume createSecretsVolume(Instance instance, StorageDriver storageDriver, String token);

}
