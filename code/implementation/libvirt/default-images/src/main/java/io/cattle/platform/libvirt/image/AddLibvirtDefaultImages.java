package io.cattle.platform.libvirt.image;

import static io.cattle.platform.core.model.tables.DataTable.*;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.model.Data;
import io.cattle.platform.core.model.Image;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.libvirt.image.lock.LibvirtDefaultImageLock;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.github.ibuildthecloud.gdapi.util.ProxyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;

public class AddLibvirtDefaultImages extends AbstractObjectProcessHandler {

    private static final String DATA_NAME_VALUE = "libvirtDefaultImages";
    private static final String LIBVIRT_KIND = "libvirt";

    URL[] images;
    String hash;
    LockManager lockManager;
    JsonMapper jsonMapper;
    AccountDao accountDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { "storagepool.activate" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        StoragePool storagePool = (StoragePool) state.getResource();

        if (!storagePool.getKind().equals(LIBVIRT_KIND)) {
            return null;
        }

        if (!upToDate(getStamp())) {
            lockManager.lock(new LibvirtDefaultImageLock(), new LockCallbackNoReturn() {
                @Override
                public void doWithLockNoResult() {
                    try {
                        loadImages();
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }
            });
        }

        return null;
    }

    protected void loadImages() throws IOException {
        Data stamp = getStamp();
        ObjectManager objectManager = getObjectManager();

        if (upToDate(stamp)) {
            return;
        }

        for (URL image : images) {
            InputStream is = null;
            try {
                is = image.openStream();
                Map<String, Object> data = jsonMapper.readValue(is);

                Object uuid = data.get(ObjectMetaDataManager.UUID_FIELD);

                if (uuid == null) {
                    throw new IllegalStateException("[" + image + "] is missing the uuid field");
                }

                Image existing = getObjectManager().findOne(Image.class, ObjectMetaDataManager.UUID_FIELD, uuid);

                if (existing != null && !existing.getState().equals(CommonStatesConstants.REQUESTED)) {
                    continue;
                }

                Image proxy = ProxyUtils.proxy(data, Image.class);

                /* Defaults */
                proxy.setInstanceKind(InstanceConstants.KIND_VIRTUAL_MACHINE);
                proxy.setIsPublic(true);
                proxy.setAccountId(accountDao.getSystemAccount().getId());

                if (existing == null) {
                    existing = objectManager.create(Image.class, data);
                }

                getObjectProcessManager().scheduleStandardProcess(StandardProcess.CREATE, existing, null);
            } finally {
                IOUtils.closeQuietly(is);
            }
        }

        if (stamp == null) {
            stamp = objectManager.newRecord(Data.class);
            stamp.setName(DATA_NAME_VALUE);
            stamp.setValue(hash);

            stamp = objectManager.create(stamp);
        } else {
            stamp.setValue(hash);
            objectManager.persist(stamp);
        }
    }

    protected Data getStamp() {
        return getObjectManager().findOne(Data.class, DATA.NAME, DATA_NAME_VALUE);
    }

    protected boolean upToDate(Data stamp) {
        return stamp != null && hash.equals(stamp.getValue());
    }

    @PostConstruct
    public void init() throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");

        for (URL url : images) {
            InputStream is = null;
            try {
                is = url.openStream();

                byte[] buffer = new byte[8192];
                int count = 0;

                while ((count = is.read(buffer)) > 0) {
                    md.update(buffer, 0, count);
                }
            } finally {
                IOUtils.closeQuietly(is);
            }
        }

        this.hash = Hex.encodeHexString(md.digest());
    }

    public URL[] getImages() {
        return images;
    }

    @Inject
    public void setImages(URL[] images) {
        this.images = images;
    }

    public LockManager getLockManager() {
        return lockManager;
    }

    @Inject
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public AccountDao getAccountDao() {
        return accountDao;
    }

    @Inject
    public void setAccountDao(AccountDao accountDao) {
        this.accountDao = accountDao;
    }

}
