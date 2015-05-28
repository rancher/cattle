package io.cattle.platform.storage.service.dao;

import io.cattle.platform.core.model.Image;

public interface ImageDao {

    Image persistImage(Image image);

}
