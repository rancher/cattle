package io.cattle.platform.storage.service.dao;

import io.cattle.platform.core.model.Image;
import io.cattle.platform.core.model.StoragePool;

public interface ImageDao {

    Image findImageByUuid(String uuid);

    Image persistAndAssociateImage(Image image, StoragePool storagePool);

}
