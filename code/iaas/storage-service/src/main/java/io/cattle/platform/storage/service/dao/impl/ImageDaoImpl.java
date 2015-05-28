package io.cattle.platform.storage.service.dao.impl;

import io.cattle.platform.core.model.Image;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.storage.service.dao.ImageDao;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageDaoImpl extends AbstractJooqDao implements ImageDao {

    private static final Logger log = LoggerFactory.getLogger(ImageDaoImpl.class);

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
