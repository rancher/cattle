package io.github.ibuildthecloud.dstack.storage.service.dao;

import io.github.ibuildthecloud.dstack.core.model.Image;
import io.github.ibuildthecloud.dstack.core.model.StoragePool;

public interface ImageDao {

    Image findImageByUuid(String uuid);

    Image persistAndAssociateImage(Image image, StoragePool storagePool);

}
