package io.cattle.platform.sample.data;

import static io.cattle.platform.core.model.tables.SettingTable.*;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Setting;
import io.cattle.platform.object.ObjectManager;


import java.util.List;

import javax.inject.Inject;


public class SampleDataStartupV17 extends AbstractSampleData {

    @Inject
    ObjectManager objectManager;

    @Override
    protected String getName() {
        return "sampleDataVersion17";
    }

    @Override
    protected void populatedData(Account system, List<Object> toCreate) {
          updateSetting();
    }

    protected void updateSetting() {
        Setting setting = objectManager.findAny(Setting.class,
                SETTING.NAME, "install.uuid");
        if (setting == null) {
            Setting installUUIDSetting = objectManager.newRecord(Setting.class);
            installUUIDSetting.setName("install.uuid");
            installUUIDSetting.setValue(java.util.UUID.randomUUID().toString());
            installUUIDSetting = objectManager.create(installUUIDSetting);
            ArchaiusUtil.refresh();
        }
    }
}