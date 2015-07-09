package io.cattle.platform.storage.service.dao.impl;

import io.cattle.platform.core.model.Image;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.storage.service.dao.ImageDao;

import javax.inject.Inject;

public class ImageDaoImpl extends AbstractJooqDao implements ImageDao {

    @Inject
    ObjectManager objectManager;
    @Inject
    ObjectProcessManager processManager;

    @Override
    public Image persistImage(Image image) {
        image = objectManager.create(image);
        processManager.scheduleStandardProcess(StandardProcess.CREATE, image, null);
        return image;
    }
}
