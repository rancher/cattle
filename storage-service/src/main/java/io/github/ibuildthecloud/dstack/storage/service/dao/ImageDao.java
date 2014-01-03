package io.github.ibuildthecloud.dstack.storage.service.dao;

import io.github.ibuildthecloud.dstack.core.model.Image;
import io.github.ibuildthecloud.dstack.core.model.StoragePool;
import io.github.ibuildthecloud.dstack.core.model.tables.records.ImageRecord;

public interface ImageDao {

    Image findImageByUuid(String uuid);

    Image persistAndAssociateImage(ImageRecord image, StoragePool storagePool);

}
