package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.DataTable.*;

import io.cattle.platform.core.dao.DataDao;
import io.cattle.platform.core.model.Data;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.util.exception.ExceptionUtils;

import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class DataDaoImpl extends AbstractJooqDao implements DataDao {

    LockManager lockManager;
    ObjectManager objectManager;

    @Override
    public String getOrCreate(final String key, final boolean visible, final Callable<String> generator) {
        Data data = objectManager.findAny(Data.class,
                DATA.NAME, key);

        if ( data != null && data.getVisible() != null && data.getVisible() == visible ) {
            return data.getValue();
        }

        return lockManager.lock(new DataChangeLock(key), new LockCallback<String>() {
            @Override
            public String doWithLock() {
                Data data = objectManager.findAny(Data.class,
                        DATA.NAME, key);

                if ( data != null && data.getVisible() != null && data.getVisible() == visible ) {
                    return data.getValue();
                } else if ( data != null ) {
                    data.setVisible(visible);
                    objectManager.persist(data);
                    return data.getValue();
                }

                try {
                    String value = generator.call();
                    if ( value == null ) {
                        return value;
                    }

                    return objectManager.create(Data.class,
                            DATA.NAME, key,
                            DATA.VISIBLE, visible,
                            DATA.VALUE, value).getValue();
                } catch (Exception e) {
                    ExceptionUtils.rethrowRuntime(e);
                    throw new RuntimeException("Failed to generate value for [" + key + "]", e);
                }
            }
        });
    }

    @Override
    public String get(String key) {
        Data data = objectManager.findAny(Data.class,
                DATA.NAME, key);
        return data == null ? null : data.getValue();
    }

    @Override
    public void save(String key, boolean visible, String value) {
        int count = create().update(DATA)
            .set(DATA.VALUE, value)
            .where(DATA.NAME.eq(key))
            .execute();
        if (count == 0) {
            objectManager.create(Data.class,
                    DATA.NAME, key,
                    DATA.VISIBLE, visible,
                    DATA.VALUE, value);
        }
    }


    public LockManager getLockManager() {
        return lockManager;
    }

    @Inject
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }
}