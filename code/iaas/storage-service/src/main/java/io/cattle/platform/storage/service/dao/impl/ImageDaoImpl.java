package io.cattle.platform.storage.service.dao.impl;

import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Image;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.storage.service.dao.ImageDao;

import javax.inject.Inject;

public class ImageDaoImpl extends AbstractJooqDao implements ImageDao {

    @Inject
    GenericResourceDao resourceDao;

    @Override
    public Image persistImage(Image image) {
        return resourceDao.createAndSchedule(image);
    }
}
