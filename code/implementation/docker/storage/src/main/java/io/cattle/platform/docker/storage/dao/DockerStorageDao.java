package io.cattle.platform.docker.storage.dao;

import io.cattle.platform.core.model.Image;
import io.cattle.platform.core.model.Instance;

public interface DockerStorageDao {

    Image createImageForInstance(Instance instance);

}
