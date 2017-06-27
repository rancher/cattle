package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.DataTable.*;

import io.cattle.platform.core.dao.DataDao;
import io.cattle.platform.core.model.Data;
import io.cattle.platform.core.model.tables.records.DataRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.util.exception.ExceptionUtils;

import java.util.concurrent.Callable;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.impl.DefaultDSLContext;

public class DataDaoImpl extends AbstractJooqDao implements DataDao {

    LockManager lockManager;
    ObjectManager objectManager;
    Configuration newConfiguration;

    public DataDaoImpl(Configuration configuration, LockManager lockManager, ObjectManager objectManager, Configuration newConfiguration) {
        super(configuration);
        this.lockManager = lockManager;
        this.objectManager = objectManager;
        this.newConfiguration = newConfiguration;
    }

    @Override
    protected DSLContext create() {
        return new DefaultDSLContext(newConfiguration);
    }

    @Override
    public String getOrCreate(final String key, final boolean visible, final Callable<String> generator) {
        Data data = create().selectFrom(DATA)
            .where(DATA.NAME.eq(key))
            .fetchAny();

        if ( data != null && data.getVisible() != null && data.getVisible() == visible ) {
            return data.getValue();
        }

        return lockManager.lock(new DataChangeLock(key), new LockCallback<String>() {
            @Override
            public String doWithLock() {
                DataRecord data = create().selectFrom(DATA)
                        .where(DATA.NAME.eq(key))
                        .fetchAny();


                if ( data != null && data.getVisible() != null && data.getVisible() == visible ) {
                    return data.getValue();
                } else if ( data != null ) {
                    data.setVisible(visible);
                    data.update();
                    return data.getValue();
                }

                try {
                    String value = generator.call();
                    if ( value == null ) {
                        return value;
                    }

                    DataRecord record = new DataRecord();
                    record.attach(newConfiguration);
                    record.setName(key);
                    record.setVisible(visible);
                    record.setValue(value);
                    record.insert();
                    return record.getValue();
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

    public ObjectManager getObjectManager() {
        return objectManager;
    }

}