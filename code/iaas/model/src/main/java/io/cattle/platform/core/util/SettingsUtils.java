package io.cattle.platform.core.util;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Setting;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.object.ObjectManager;

import javax.inject.Inject;

public class SettingsUtils {

    @Inject
    ObjectManager objectManager;

    public void changeSetting(String name, Object value) {
        if (name == null) {
            return;
        }

        if (value == null) {
            value = "";
        }

        Setting setting = objectManager.findOne(Setting.class, "name", name);

        if (setting == null) {
            objectManager.create(Setting.class, "name", name, "value", value);
        } else {
            objectManager.setFields(setting, "value", value);
        }

        DeferredUtils.defer(new Runnable() {
            @Override
            public void run() {
                ArchaiusUtil.refresh();
            }
        });
    }


}
