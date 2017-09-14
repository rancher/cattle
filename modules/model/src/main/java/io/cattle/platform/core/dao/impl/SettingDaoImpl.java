package io.cattle.platform.core.dao.impl;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.dao.SettingDao;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.framework.event.FrameworkEvents;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.util.TransactionDelegate;
import org.apache.cloudstack.managed.threadlocal.ManagedThreadLocal;
import org.jooq.Configuration;

import static io.cattle.platform.core.model.Tables.*;

public class SettingDaoImpl extends AbstractJooqDao implements SettingDao {

    private static final ManagedThreadLocal<Boolean> ONCE = new ManagedThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    ObjectManager objectManager;
    EventService eventService;
    TransactionDelegate transaction;

    public SettingDaoImpl(Configuration configuration, TransactionDelegate transaction, ObjectManager objectManager, EventService eventService) {
        super(configuration);
        this.transaction = transaction;
        this.objectManager = objectManager;
        this.eventService = eventService;
    }

    @Override
    public void setValue(String name, Object value) {
        if (value == null) {
            deleteSetting(name);
            return;
        }

        if (ArchaiusUtil.isFixed(name)) {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN,
                    "LockedSetting",
                    "Setting is locked",
                    "Setting is locked to the value " + ArchaiusUtil.getStringValue(name));
        }

        transaction.doInTransaction(() -> {
            int count = create().update(SETTING)
                    .set(SETTING.VALUE, value.toString())
                    .where(SETTING.NAME.eq(name))
                    .execute();
            if (count == 0) {
                create().insertInto(SETTING, SETTING.NAME, SETTING.VALUE)
                        .values(name, value.toString())
                        .execute();
            }
        });

        notifySettingsChanged();
    }

    @Override
    public void deleteSetting(String name) {
        transaction.doInTransaction(() -> {
            create().delete(SETTING)
                    .where(SETTING.NAME.eq(name))
                    .execute();
        });

        notifySettingsChanged();
    }

    public void notifySettingsChanged() {
        DeferredUtils.defer(new Runnable() {
            @Override
            public void run() {
                if (ONCE.get()) {
                    return;
                }
                ONCE.set(true);
                eventService.publish(new EventVO<>(FrameworkEvents.SETTINGS_CHANGE));
            }
        });
    }
}
